@Deprecated("text")
annotation class obsolete()

@Deprecated("text")
annotation class obsoleteWithParam(val text: String)

@obsolete class Obsolete

@obsoleteWithParam("text") class Obsolete2
