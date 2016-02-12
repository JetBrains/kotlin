interface  A1() {
    constructor(x: Int = "", y: Int) : this() {
        x + y
    }
}

interface  A2 private constructor(private val prop: Int) {
    constructor(x: Int = "", y: Int) : this(x * y) {
        x + y
    }
}

//public interface A1 defined in root package
//public constructor A1() defined in A1
//public constructor A1(x: kotlin.Int = ..., y: kotlin.Int) defined in A1
//value-parameter x: kotlin.Int = ... defined in A1.<init>
//value-parameter y: kotlin.Int defined in A1.<init>
//public interface A2 defined in root package
//private constructor A2(prop: kotlin.Int) defined in A2
//value-parameter prop: kotlin.Int defined in A2.<init>
//public constructor A2(x: kotlin.Int = ..., y: kotlin.Int) defined in A2
//value-parameter x: kotlin.Int = ... defined in A2.<init>
//value-parameter y: kotlin.Int defined in A2.<init>
