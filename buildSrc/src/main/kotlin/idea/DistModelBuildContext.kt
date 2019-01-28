package idea

import org.gradle.api.Action
import org.gradle.api.file.FileCopyDetails
import org.jetbrains.kotlin.buildUtils.idea.DistVFile
import org.jetbrains.kotlin.buildUtils.idea.logger

/**
 * Used for logging and nesting properties
 */
class DistModelBuildContext(
        val parent: DistModelBuildContext?,
        val kind: String,
        val title: String,
        val report: Appendable? = parent?.report,
        val shade: Boolean = parent?.shade ?: false
) {
    val logEnabled = false
    val allCopyActions = mutableSetOf<Action<in FileCopyDetails>>()

    var destination: DistVFile? = parent?.destination // todo: don't nest destination between tasks visiting
    val logPrefix: String = if (parent != null) "${parent.logPrefix}-" else ""

    init {
        report?.appendln(toString())
        if (parent != null) {
            allCopyActions.addAll(parent.allCopyActions)
        }
    }

    fun log(kind: String, title: String = "", print: Boolean = false) {
        if (logEnabled) {
            report?.appendln("$logPrefix- $kind $title")
            if (print) {
                logger.error("$kind $title, while visiting:")
                var p = this
                while (p.parent != null) {
                    logger.error(" - ${p.kind} ${p.title}")
                    p = p.parent!!
                }
            }
        }
    }

    fun logUnsupported(kind: String, obj: Any? = null) {
        val objInfo = if (obj != null) {
            val javaClass = obj.javaClass
            val superclass = javaClass.superclass as Class<*>
            "$obj [$javaClass extends $superclass implements ${javaClass.interfaces.map { it.canonicalName }}]"
        } else ""

        log("UNSUPPORTED $kind", objInfo, true)
    }

    override fun toString() = "$logPrefix $kind $title"

    inline fun child(
            kind: String,
            title: String = "",
            shade: Boolean = false,
            body: (DistModelBuildContext) -> Unit = {}
    ): DistModelBuildContext {
        val result = DistModelBuildContext(this, kind, title, shade = shade)
        body(result)
        return result
    }

    fun addCopyActions(allCopyActions: Collection<Action<in FileCopyDetails>>) {
        allCopyActions.forEach {
            log("COPY ACTION", "$it")
        }

        this.allCopyActions.addAll(allCopyActions)
    }
}

