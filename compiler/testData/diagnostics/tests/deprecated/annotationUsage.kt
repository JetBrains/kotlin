@Deprecated("text")
annotation class obsolete()

@Deprecated("text")
annotation class obsoleteWithParam(val text: String)

@<!DEPRECATION!>obsolete<!> class Obsolete

@<!DEPRECATION!>obsoleteWithParam<!>("text") class Obsolete2
