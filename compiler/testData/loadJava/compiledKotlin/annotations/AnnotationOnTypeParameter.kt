package test

// See: KT-52157

@Target(AnnotationTarget.TYPE_PARAMETER)
public annotation class Schema

class Convert<@Schema T, C>()
