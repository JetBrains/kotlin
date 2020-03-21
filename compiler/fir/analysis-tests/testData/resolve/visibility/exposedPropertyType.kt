class A {
    private class AInnerPrivate(val str: String) {

    }

    protected enum class AInnerProtectedEnum {
        A,
        B
    }

    public class AInnerPublic(val str: String) {

    }
}

class Property {
    var var1: String
    var var2: String
    var var3: Int
    <!FIR_EXPOSED_TYPEALIAS_EXPANDED_TYPE!>var var4: A.AInnerPrivate<!>
    var var5: A.AInnerPublic
    <!FIR_EXPOSED_TYPEALIAS_EXPANDED_TYPE!>var var6: A.AInnerProtectedEnum<!>
}