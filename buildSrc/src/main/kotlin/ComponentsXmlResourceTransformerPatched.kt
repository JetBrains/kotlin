/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import com.github.jengelman.gradle.plugins.shadow.relocation.RelocateClassContext
import com.github.jengelman.gradle.plugins.shadow.transformers.Transformer
import com.github.jengelman.gradle.plugins.shadow.transformers.TransformerContext
import org.gradle.api.file.FileTreeElement
import shadow.org.apache.tools.zip.ZipEntry
import shadow.org.apache.tools.zip.ZipOutputStream
import shadow.org.codehaus.plexus.util.IOUtil
import shadow.org.codehaus.plexus.util.ReaderFactory
import shadow.org.codehaus.plexus.util.WriterFactory
import shadow.org.codehaus.plexus.util.xml.Xpp3Dom
import shadow.org.codehaus.plexus.util.xml.Xpp3DomBuilder
import shadow.org.codehaus.plexus.util.xml.Xpp3DomWriter
import java.io.*
import java.lang.Exception
import java.util.LinkedHashMap

/**
 * A resource processor that aggregates plexus `components.xml` files.
 *
 * Fixed version of [com.github.jengelman.gradle.plugins.shadow.transformers.ComponentsXmlResourceTransformer],
 * may be dropped after [the fix in ShadowJAR](https://github.com/johnrengelman/shadow/pull/678/files) will be accepted
 */
class ComponentsXmlResourceTransformerPatched : Transformer {
    private val components: MutableMap<String, Xpp3Dom> =
        LinkedHashMap<String, Xpp3Dom>()

    override fun getName() = "ComponentsXmlResourceTransformerPatched"

    override fun canTransformResource(element: FileTreeElement): Boolean {
        val path = element.relativePath.pathString
        return COMPONENTS_XML_PATH == path
    }

    override fun transform(context: TransformerContext) {
        val newDom: Xpp3Dom = try {
            val bis: BufferedInputStream = object : BufferedInputStream(context.getIs()) {
                override fun close() {
                    // leave ZIP open
                }
            }
            val reader: Reader = ReaderFactory.newXmlReader(bis)
            Xpp3DomBuilder.build(reader)
        } catch (e: Exception) {
            throw (IOException("Error parsing components.xml in " + context.getIs()).initCause(e) as IOException)
        }

        // Only try to merge in components if there are some elements in the component-set
        if (newDom.getChild("components") == null) {
            return
        }
        val children: Array<Xpp3Dom>? = newDom.getChild("components")?.getChildren("component")
        children?.forEach { component ->
            var role: String? = getValue(component, "role")
            role = getRelocatedClass(role, context)
            setValue(component, "role", role)
            val roleHint = getValue(component, "role-hint")
            var impl: String? = getValue(component, "implementation")
            impl = getRelocatedClass(impl, context)
            setValue(component, "implementation", impl)
            val key = "$role:$roleHint"
            if (components.containsKey(key)) {
                // configuration carry over
                val dom: Xpp3Dom? = components[key]
                if (dom?.getChild("configuration") != null) {
                    component.addChild(dom.getChild("configuration"))
                }
            }
            val requirements: Xpp3Dom? = component.getChild("requirements")
            if (requirements != null && requirements.childCount > 0) {
                for (r in requirements.childCount - 1 downTo 0) {
                    val requirement: Xpp3Dom = requirements.getChild(r)
                    var requiredRole: String? = getValue(requirement, "role")
                    requiredRole = getRelocatedClass(requiredRole, context)
                    setValue(requirement, "role", requiredRole)
                }
            }
            components[key] = component
        }
    }

    override fun modifyOutputStream(os: ZipOutputStream, preserveFileTimestamps: Boolean) {
        val data = transformedResource
        val entry = ZipEntry(COMPONENTS_XML_PATH)
        entry.time = TransformerContext.getEntryTimestamp(preserveFileTimestamps, entry.time)
        os.putNextEntry(entry)
        IOUtil.copy(data, os)
        components.clear()
    }

    override fun hasTransformedResource(): Boolean {
        return components.isNotEmpty()
    }

    private val transformedResource: ByteArray
        get() {
            val baos = ByteArrayOutputStream(1024 * 4)
            val writer: Writer = WriterFactory.newXmlWriter(baos)
            try {
                val dom = Xpp3Dom("component-set")
                val componentDom = Xpp3Dom("components")
                dom.addChild(componentDom)
                for (component in components.values) {
                    componentDom.addChild(component)
                }
                Xpp3DomWriter.write(writer, dom)
            } finally {
                writer.close()
            }
            return baos.toByteArray()
        }

    companion object {
        private const val COMPONENTS_XML_PATH = "META-INF/plexus/components.xml"
        private fun getRelocatedClass(className: String?, context: TransformerContext): String? {
            val relocators = context.relocators
            val stats = context.stats
            if (className != null && className.isNotEmpty() && relocators != null) {
                for (relocator in relocators) {
                    if (relocator.canRelocateClass(className)) {
                        val relocateClassContext = RelocateClassContext(className, stats)
                        return relocator.relocateClass(relocateClassContext)
                    }
                }
            }
            return className
        }

        private fun getValue(dom: Xpp3Dom, element: String): String {
            val child: Xpp3Dom? = dom.getChild(element)
            return if (child?.value != null) child.value else ""
        }

        private fun setValue(dom: Xpp3Dom, element: String, value: String?) {
            val child: Xpp3Dom? = dom.getChild(element)
            if (value == null || value.isEmpty()) {
                return
            }
            child?.value = value
        }
    }
}
