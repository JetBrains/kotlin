package rendererTest

private enum class TheEnum(val rgb: Int) {
    VAL1: TheEnum(0xFF0000)
}

//package rendererTest defined in root package
//private final enum class TheEnum : jet.Enum<rendererTest.TheEnum> defined in rendererTest
//private constructor TheEnum(rgb: jet.Int) defined in rendererTest.TheEnum
//value-parameter val rgb: jet.Int defined in rendererTest.TheEnum.<init>
//private enum entry VAL1 : rendererTest.TheEnum defined in rendererTest.TheEnum.<class-object-for-TheEnum>
//private constructor VAL1() defined in rendererTest.TheEnum.VAL1
//public final val VAL1: rendererTest.TheEnum defined in rendererTest.TheEnum.<class-object-for-TheEnum>
