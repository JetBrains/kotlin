package rendererTest

private enum class TheEnum(val rgb : Int) {
    VAL1 : TheEnum(0xFF0000)
}

//package rendererTest defined in root package
//private final enum class TheEnum defined in rendererTest
//value-parameter val rgb : jet.Int defined in rendererTest.TheEnum.<init>
//internal final class VAL1 : rendererTest.TheEnum defined in rendererTest.TheEnum.<class-object-for-TheEnum>
//internal final val VAL1 : rendererTest.TheEnum.<class-object-for-TheEnum>.VAL1 defined in rendererTest.TheEnum.<class-object-for-TheEnum>