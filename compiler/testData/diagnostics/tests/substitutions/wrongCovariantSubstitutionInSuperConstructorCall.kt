open class Base<T>(val m: T)

class Bad<K>(m: K) : Base<<!PROJECTION_IN_IMMEDIATE_ARGUMENT_TO_SUPERTYPE!>out<!> K>(m)