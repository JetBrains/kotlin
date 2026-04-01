// FULL_JDK
// TARGET_BACKEND: JVM
// FILE: test.kt

import java.util.function.Function

fun box(): String {

    Function<String, String> { Function<String, String> { it }.apply("") }.apply("")

    Function<String, String> { Function<String, String> { it }
        .apply("") }
        .apply("")

    Function<String, String> {
        Function<String, String> {
            it
        }.apply("")
    }.apply("")

    Function<String, String> {
        Function<String, String> {
            it
        }
            .apply ("")
    }
        .apply ("")

    Function<String, String> {
            it: String ->
        Function<String, String> {
            innerIt: String ->
            innerIt
        }.apply("")
    }.apply("")

    Function<String, String> {
        it: String ->
        Function<String, String> {
            innerIt: String ->
            innerIt
        }
            .apply("")
    }
        .apply("")

    Function<String, String>()
    {
        it: String ->
        Function<String, String>()
        {
            innerIt: String ->
            innerIt
        }.apply("")
    }.apply("")

    Function<String, String>()
    {
        it: String ->
        Function<String, String>()
        {
            innerIt: String ->
            innerIt
        }
            .apply("")
    }
        .apply("")

    return "OK"
}

// EXPECTATIONS JVM_IR
// test.kt:9 box
// test.kt:9 box$lambda$0$0
// test.kt:9 box$lambda$0
// test.kt:9 box

// test.kt:11 box
// test.kt:13 box
// test.kt:11 box$lambda$1$0
// test.kt:12 box$lambda$1
// test.kt:13 box

// test.kt:15 box
// test.kt:19 box
// test.kt:17 box$lambda$2$0
// test.kt:18 box$lambda$2
// test.kt:19 box

// test.kt:21 box
// test.kt:27 box
// test.kt:23 box$lambda$3$0
// test.kt:25 box$lambda$3
// test.kt:27 box

// test.kt:29 box
// test.kt:35 box
// test.kt:31 box$lambda$4
// test.kt:34 box$lambda$4
// test.kt:33 box$lambda$4$0
// test.kt:34 box$lambda$4
// test.kt:35 box

// test.kt:37 box
// test.kt:45 box
// test.kt:39 box$lambda$5
// test.kt:43 box$lambda$5
// test.kt:41 box$lambda$5$0
// test.kt:43 box$lambda$5
// test.kt:45 box

// test.kt:47 box
// test.kt:55 box
// test.kt:50 box$lambda$6
// test.kt:54 box$lambda$6
// test.kt:53 box$lambda$6$0
// test.kt:54 box$lambda$6
// test.kt:55 box

// test.kt:57 box
// test.kt:67 box
// test.kt:60 box$lambda$7
// test.kt:65 box$lambda$7
// test.kt:63 box$lambda$7$0
// test.kt:65 box$lambda$7
// test.kt:67 box

// test.kt:69 box
