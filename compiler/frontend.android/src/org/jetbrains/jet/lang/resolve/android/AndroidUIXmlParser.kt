package org.jetbrains.jet.lang.resolve.android

import org.jetbrains.jet.lang.psi.JetFile
import java.util.ArrayList
import javax.xml.parsers.SAXParserFactory
import java.io.File
import java.io.FileInputStream
import com.intellij.openapi.project.Project
import org.jetbrains.jet.lang.psi.JetPsiFactory

class AndroidUIXmlParser(val project: Project?, val searchPaths: Collection<File>) {

    inner class NoUIXMLsFound: Exception("No android UI xmls found in ${searchPaths.joinToString(", ")}")

    val ids: MutableCollection<AndroidWidget> = ArrayList()
    val kw = KotlinStringWriter()
    val androidImports = arrayListOf("android.app.Activity",
                                     "android.view.View",
                                     "android.widget.*")

    public fun parseToString(): String? {
        return doParse()
    }

    public fun parseToPsi(): JetFile? {
        val s = parseToString()
        return if (s != null) JetPsiFactory(project).createFile(s) else null
    }

    private fun isAndroidUIXml(file: File): Boolean {
        return file.extension == "xml"
    }

    private fun searchForUIXml(paths: Collection<File>?): Collection<File> {
        if (paths == null) return ArrayList(0)
        val res = ArrayList<File>()
        for (path in paths) {
            if (path.isFile() && isAndroidUIXml(path)) {
                res.add(path)
            } else if (path.isDirectory()) {
                res.addAll(searchForUIXml(path.listFiles()?.toArrayList()))
            }
        }
        return res
    }

    private fun writeImports() {
        for (elem in androidImports)
            kw.writeImport(elem)
        kw.writeEmptyLine()
    }

    private fun doParse(): String? {
        if (searchPaths.empty) return null
        val xmlStreams = searchForUIXml(searchPaths).map { FileInputStream(it) }
        val factory = SAXParserFactory.newInstance()
        factory?.setNamespaceAware(true)
        val parser = factory!!.newSAXParser() // TODO: annotate this
        val handler = AndroidXmlHandler({ id, wClass -> widgetCallback(id, wClass) })
        writeImports()
        for (xmlStream in xmlStreams) {
            parser.parse(xmlStream, handler)
        }
        return produceKotlinSignatures().toString()
    }

    private fun widgetCallback(id: String, className: String) {
        ids.add(AndroidWidget(id, className))
    }

    private fun produceKotlinSignatures(): StringBuffer {
        for (id in ids) {
            val body = arrayListOf("return findViewById(R.id.${id.id}) as ${id.className}")
            kw.writeImmutableExtensionProperty(receiver = "Activity",
                                      name = id.id,
                                      retType = id.className,
                                      getterBody = body )
        }
        return kw.output()
    }

}
