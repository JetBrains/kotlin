package research.kapt.gen

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.writeText

object Generator {

    fun generate(
        count: Int,
        mode: Mode,
        seed: Long,
        outDir: Path,
        wideMemberCount: Int? = null,
        nestedCount: Int? = null,
        packageDepth: Int? = null,
        commonBase: Boolean = false,
        bodyMethodCount: Int? = null,
    ) {
        val shapeOverrides = listOfNotNull(wideMemberCount, nestedCount, bodyMethodCount)
        require(shapeOverrides.isEmpty() || mode == Mode.SYNTHETIC) {
            "--wide/--nested/--bodies are only supported for mode=SYNTHETIC (other modes generate cross-referencing extras)"
        }
        require(shapeOverrides.size <= 1) { "--wide, --nested and --bodies are mutually exclusive" }
        // seed is reserved for future randomization of shape mix; output is currently deterministic by index alone.
        Files.createDirectories(outDir)
        // Extra package components deepen the output directory tree without changing the class bodies,
        // so this composes with (and is independent of) the wide/nested shapes.
        val pkg = buildString {
            append("research.kapt.generated")
            for (i in 0 until (packageDepth ?: 0)) append(".d").append(i)
        }
        val pkgDir = outDir.resolve(pkg.replace('.', '/'))
        Files.createDirectories(pkgDir)

        // Imports differ per mode; we always write the import header, even if some imports are unused.
        val header = buildString {
            appendLine("package $pkg")
            appendLine()
            when (mode) {
                Mode.SYNTHETIC -> appendLine("import research.kapt.SyntheticAnno")
                Mode.DAGGER -> {
                    appendLine("import javax.inject.Inject")
                    appendLine("import dagger.Module")
                    appendLine("import dagger.Provides")
                    appendLine("import dagger.Component")
                }
                Mode.MAPSTRUCT -> appendLine("import org.mapstruct.Mapper")
            }
            appendLine()
        }

        // Generate body files
        for (i in 0 until count) {
            val name = "Gen$i"
            val shape = shapeFor(i)
            val body = when {
                wideMemberCount != null -> renderWideClass(name, mode, wideMemberCount)
                nestedCount != null -> renderNestedClass(name, mode, nestedCount)
                bodyMethodCount != null -> renderBodyHeavyClass(name, mode, bodyMethodCount)
                mode == Mode.MAPSTRUCT && i % 5 == 0 && i > 0 -> renderMapstructMapper(i)
                else -> renderShape(name, shape, mode)
            }
            // One shared supertype reference per file, so a single ABI change to `Base` impacts every
            // generated type — what the incremental scenario needs to reach the O(n^2) bookkeeping.
            val baseUse = if (commonBase) "\n@SyntheticAnno\nclass ${name}_UsesBase constructor(val base: research.kapt.base.Base)\n" else ""
            pkgDir.resolve("$name.kt").writeText(header + body + baseUse)
        }

        // Mode-specific extras
        when (mode) {
            Mode.DAGGER -> {
                pkgDir.resolve("DaggerWiring.kt").writeText(
                    header + renderDaggerModulesAndComponent(count)
                )
            }
            Mode.MAPSTRUCT, Mode.SYNTHETIC -> { /* no extras */ }
        }
    }

    private fun renderMapstructMapper(i: Int): String {
        // Each mapper file also declares a companion _Data class (so Gen${i}_Data always exists),
        // and the mapper converts from the nearest preceding companion data class to the current one.
        // Preceding data class is at i-5 (also a mapper file with its own _Data companion).
        // When i==5, the preceding data class is Gen0_Data (generated as a plain DATA_CLASS shape).
        val src = i - 5
        return """
        |data class Gen${i}_Data constructor(val a: Int, val b: String, val c: Long)
        |
        |@Mapper
        |interface Gen${i}_Mapper {
        |    fun map(src: Gen${src}_Data): Gen${i}_Data
        |}
        |""".trimMargin()
    }

    private fun renderDaggerModulesAndComponent(count: Int): String {
        val moduleProvides = (0 until count step 50).joinToString("\n") { i ->
            """
            |    @Provides
            |    fun provideGen${i}_Data(): Gen${i}_Data = Gen${i}_Data(0, "x", 0L)
            """.trimMargin()
        }
        return """
        |@Module
        |class GenModule {
        |$moduleProvides
        |}
        |
        |@Component(modules = [GenModule::class])
        |interface GenComponent
        |""".trimMargin()
    }
}
