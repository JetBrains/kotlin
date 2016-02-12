object A1() {
    constructor(x: Int = "", y: Int) : this() {
        x + y
    }
}

object A2 public constructor(private val prop: Int) {
    constructor(x: Int = "", y: Int) : this(x * y) {
        x + y
    }
}

class A3 {
    companion object B(val prop: Int) {
        public constructor() : this(2)
    }
}

//public object A1 defined in root package
//private constructor A1() defined in A1
//private constructor A1(x: kotlin.Int = ..., y: kotlin.Int) defined in A1
//value-parameter x: kotlin.Int = ... defined in A1.<init>
//value-parameter y: kotlin.Int defined in A1.<init>
//public object A2 defined in root package
//public constructor A2(prop: kotlin.Int) defined in A2
//value-parameter prop: kotlin.Int defined in A2.<init>
//private constructor A2(x: kotlin.Int = ..., y: kotlin.Int) defined in A2
//value-parameter x: kotlin.Int = ... defined in A2.<init>
//value-parameter y: kotlin.Int defined in A2.<init>
//public final class A3 defined in root package
//public constructor A3() defined in A3
//public companion object B defined in A3
//private constructor B(prop: kotlin.Int) defined in A3.B
//value-parameter prop: kotlin.Int defined in A3.B.<init>
//public constructor B() defined in A3.B
