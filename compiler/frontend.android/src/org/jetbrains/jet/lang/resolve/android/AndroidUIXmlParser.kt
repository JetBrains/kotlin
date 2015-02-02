package org.jetbrains.jet.lang.resolve.android

import java.util.ArrayList
import javax.xml.parsers.SAXParserFactory
import java.io.File
import java.io.FileInputStream
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.psi.JetPsiFactory
import org.jetbrains.kotlin.psi.JetFile

class AndroidUIXmlParser(val project: Project?, val searchPaths: Collection<File>) {

    val ids: MutableCollection<AndroidWidget> = ArrayList()
    val kw = KotlinStringWriter()
    val androidImports = arrayListOf("android.app.Activity",
                                     "android.view.View",
                                     "android.widget.*")

    public fun parse(): String {
        doParse()
        return produceKotlinSignatures().toString()
    }

    public fun parseToPsi(): JetFile {
        return JetPsiFactory(project).createFile(parse())
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
        val handler = AndroidXmlHandler({ id, wClass -> widgetCallback(id, wClass) })
        writeImports()
        for (xmlStream in xmlStreams) {
            parser.parse(xmlStream, handler)
        }
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
