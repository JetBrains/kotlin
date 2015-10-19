@file:StringHolder("OK")

fun box(): String =
        Class.forName("FileFacadeKt").getAnnotation(StringHolder::class.java)?.value ?: "null"