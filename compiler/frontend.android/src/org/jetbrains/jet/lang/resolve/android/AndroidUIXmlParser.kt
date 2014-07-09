package org.jetbrains.jet.lang.resolve.android

import java.nio.file.Path
import org.jetbrains.jet.lang.psi.JetFile
import java.util.ArrayList
import javax.xml.parsers.SAXParserFactory
import java.io.File
import java.io.FileInputStream
import com.intellij.openapi.project.Project
import org.jetbrains.jet.lang.psi.JetPsiFactory

class AndroidUIXmlParser(val project: Project?, val searchPaths: Collection<File>) {

    val ids: MutableCollection<AndroidID> = ArrayList()
    val kw = KotlinStringWriter()
    val androidImports = arrayListOf("android.app.Activity",
                                     "android.view.View")

    public fun parse(): String {
        doParse()
        return produceKotlinSignatures().toString()
    }

    public fun parseToPsi(): JetFile {
        return JetPsiFactory.createFile(project, parse())
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

    private fun doParse() {
        val xmlStreams = searchForUIXml(searchPaths).map { FileInputStream(it) }
        val factory = SAXParserFactory.newInstance()
        factory?.setNamespaceAware(true)
        val parser = factory!!.newSAXParser() // TODO: annotate this
        val handler = AndroidXmlHandler({ id -> ids.add(AndroidID(id)) })
        writeImports()
        for (xmlStream in xmlStreams) {
            parser.parse(xmlStream, handler)
        }
    }

    private fun produceKotlinSignatures(): StringBuffer {
        for (id in ids) {
            val body = arrayListOf("return findViewById(R.id.${id.toString()})!!")
            kw.writeImmutableExtensionProperty(receiver = "Activity",
                                      name = id.toString(),
                                      retType = "View",
                                      getterBody = body )
        }
        return kw.output()
    }

}
