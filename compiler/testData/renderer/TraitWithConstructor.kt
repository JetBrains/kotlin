trait A1() {
    constructor(x: Int = "", y: Int) : this() {
        x + y
    }
}

trait A2 private constructor(private val prop: Int) {
    constructor(x: Int = "", y: Int) : this(x * y) {
        x + y
    }
}

//internal interface A1 defined in root package
//public constructor A1() defined in A1
//public constructor A1(x: kotlin.Int = ..., y: kotlin.Int) defined in A1
//value-parameter val x: kotlin.Int = ... defined in A1.<init>
//value-parameter val y: kotlin.Int defined in A1.<init>
//internal interface A2 defined in root package
//private constructor A2(prop: kotlin.Int) defined in A2
//value-parameter val prop: kotlin.Int defined in A2.<init>
//public constructor A2(x: kotlin.Int = ..., y: kotlin.Int) defined in A2
//value-parameter val x: kotlin.Int = ... defined in A2.<init>
//value-parameter val y: kotlin.Int defined in A2.<init>
