import com.github.jengelman.gradle.plugins.shadow.transformers.Transformer
import com.github.jengelman.gradle.plugins.shadow.transformers.TransformerContext
import org.gradle.api.file.FileTreeElement
import org.gradle.api.logging.Logger
import org.jetbrains.kotlin.serialization.jvm.JvmPackageTable
import shadow.org.apache.tools.zip.ZipEntry
import shadow.org.apache.tools.zip.ZipOutputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream

class KotlinModuleShadowTransformer(private val logger: Logger) : Transformer {
    private data class Entry(val path: String, val bytes: ByteArray)
    private val data = mutableListOf<Entry>()

    override fun canTransformResource(element: FileTreeElement): Boolean =
            element.path.substringAfterLast(".") == KOTLIN_MODULE

    override fun transform(context: TransformerContext) {
        fun relocate(content: String): String =
                context.relocators.fold(content) { acc, relocator -> relocator.applyToSourceContent(acc) }

        val input = DataInputStream(context.`is`)
        val version = IntArray(input.readInt()) { input.readInt() }
        logger.info("Transforming ${context.path} with version ${version.toList()}")

        val table = JvmPackageTable.PackageTable.parseFrom(context.`is`).toBuilder()

        val newTable = JvmPackageTable.PackageTable.newBuilder().apply {
            for (packageParts in table.packagePartsList + table.metadataPartsList) {
                addPackageParts(JvmPackageTable.PackageParts.newBuilder(packageParts).apply {
                    packageFqName = relocate(packageFqName)
                })
            }
            addAllJvmPackageName(table.jvmPackageNameList.map(::relocate))
        }

        val baos = ByteArrayOutputStream()
        val output = DataOutputStream(baos)
        output.writeInt(version.size)
        version.forEach(output::writeInt)
        newTable.build().writeTo(output)
        output.flush()

        data += Entry(context.path, baos.toByteArray())
    }

    override fun hasTransformedResource(): Boolean =
            data.isNotEmpty()

    override fun modifyOutputStream(os: ZipOutputStream) {
        for ((path, bytes) in data) {
            os.putNextEntry(ZipEntry(path))
            os.write(bytes)
        }
        data.clear()
    }

    companion object {
        const val KOTLIN_MODULE = "kotlin_module"
    }
}
