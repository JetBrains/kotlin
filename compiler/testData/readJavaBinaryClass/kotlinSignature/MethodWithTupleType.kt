package test

import java.util.*

public open class MethodWithTupleType : Object() {
    open fun foo(p0 : Tuple2<String, String?>) { // writing Tuple2<..> instead of #(..), because the latter
                                                 // adds unnecessary "out" keywords
    }
}
