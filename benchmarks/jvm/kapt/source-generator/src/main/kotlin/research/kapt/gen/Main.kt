package research.kapt.gen

import java.nio.file.Path

fun main(argv: Array<String>) {
    val args = argv.toList().chunked(2).associate { it[0] to it[1] }
    val count = (args["--count"] ?: error("--count required")).toInt()
    val mode = Mode.valueOf((args["--mode"] ?: error("--mode required")).uppercase())
    val seed = (args["--seed"] ?: "42").toLong()
    val outDir = Path.of(args["--out-dir"] ?: error("--out-dir required"))
    val wide = args["--wide"]?.toInt()
    val nested = args["--nested"]?.toInt()
    val packageDepth = args["--package-depth"]?.toInt()
    val commonBase = args["--common-base"]?.toBoolean() ?: false
    val bodies = args["--bodies"]?.toInt()
    Generator.generate(count, mode, seed, outDir, wide, nested, packageDepth, commonBase, bodies)
    println(
        "Generated $count files in $outDir " +
                "(mode=$mode, seed=$seed, wide=${wide ?: "off"}, nested=${nested ?: "off"}, " +
                "packageDepth=${packageDepth ?: "off"}, commonBase=$commonBase, bodies=${bodies ?: "off"})"
    )
}
