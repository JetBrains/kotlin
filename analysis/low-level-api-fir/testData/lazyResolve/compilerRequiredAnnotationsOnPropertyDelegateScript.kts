annotation class Anno(val s: String)

@delegate:Deprecated("delegate")
@delegate:Anno("delegate")
val memberP<caret>roperty by lazy {
    "42"
}