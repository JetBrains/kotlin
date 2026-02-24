// FILE: test.kt

fun interface FunInterface {
    fun bar(p: String): String
}

fun box(): String {

    FunInterface { FunInterface{ it }.bar("") }.bar("")

    FunInterface { FunInterface { it }
        .bar("") }
        .bar("")

    FunInterface {
        FunInterface {
            it
        }.bar("")
    }.bar("")

    FunInterface {
        FunInterface {
            it
        }
            .bar("")
    }
        .bar("")

    FunInterface {
        it: String ->
        FunInterface {
            innerIt: String ->
            innerIt
        }.bar("")
    }.bar("")

    FunInterface {
        it: String ->
        FunInterface {
            innerIt: String ->
            innerIt
        }
            .bar("")
    }
        .bar("")

    FunInterface()
    {
        it: String ->
        FunInterface()
        {
            innerIt: String ->
            innerIt
        }.bar("")
    }.bar("")

    FunInterface()
    {
        it: String ->
        FunInterface()
        {
            innerIt: String ->
            innerIt
        }
            .bar("")
    }
        .bar("")

    return "OK"
}

// EXPECTATIONS JVM_IR
// test.kt:9 box
// test.kt:9 box$lambda$0
// test.kt:9 box$lambda$0$0
// test.kt:9 box$lambda$0
// test.kt:9 box

// test.kt:11 box
// test.kt:13 box
// test.kt:11 box$lambda$1
// test.kt:12 box$lambda$1
// test.kt:11 box$lambda$1$0
// test.kt:12 box$lambda$1
// test.kt:13 box

// test.kt:15 box
// test.kt:19 box
// test.kt:16 box$lambda$2
// test.kt:18 box$lambda$2
// test.kt:17 box$lambda$2$0
// test.kt:18 box$lambda$2
// test.kt:19 box

// test.kt:21 box
// test.kt:27 box
// test.kt:22 box$lambda$3
// test.kt:25 box$lambda$3
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

// EXPECTATIONS WASM
// test.kt:9 $box (17, 48, 52, 48)
// test.kt:9 $box$lambda.invoke (31, 38, 42, 38)
// test.kt:9 $box$lambda$lambda.invoke (33, 35)
// test.kt:9 $box$lambda.invoke (45)
// test.kt:9 $box (48)

// test.kt:11 $box (17)
// test.kt:13 $box (9, 13, 9)
// test.kt:11 $box$lambda.invoke (32)
// test.kt:12 $box$lambda.invoke (9, 13, 9)
// test.kt:11 $box$lambda$lambda.invoke (34, 36)
// test.kt:12 $box$lambda.invoke (16)
// test.kt:13 $box (9)

// test.kt:15 $box (17)
// test.kt:19 $box (6, 10, 6)
// test.kt:16 $box$lambda.invoke (21)
// test.kt:18 $box$lambda.invoke (10, 14, 10)
// test.kt:17 $box$lambda$lambda.invoke (12, 14)
// test.kt:18 $box$lambda.invoke (17)
// test.kt:19 $box (6)

// test.kt:21 $box (17)
// test.kt:27 $box (9, 13, 9)
// test.kt:22 $box$lambda.invoke (21)
// test.kt:25 $box$lambda.invoke (13, 17, 13)
// test.kt:23 $box$lambda$lambda.invoke (12, 14)
// test.kt:25 $box$lambda.invoke (20)
// test.kt:27 $box (9)

// test.kt:29 $box (17)
// test.kt:35 $box (6, 10, 6)
// test.kt:31 $box$lambda.invoke (21)
// test.kt:34 $box$lambda.invoke (10, 14, 10)
// test.kt:33 $box$lambda$lambda.invoke (12, 19)
// test.kt:34 $box$lambda.invoke (17)
// test.kt:35 $box (6)

// test.kt:37 $box (17)
// test.kt:45 $box (9, 13, 9)
// test.kt:39 $box$lambda.invoke (21)
// test.kt:43 $box$lambda.invoke (13, 17, 13)
// test.kt:41 $box$lambda$lambda.invoke (12, 19)
// test.kt:43 $box$lambda.invoke (20)
// test.kt:45 $box (9)

// test.kt:48 $box (4)
// test.kt:55 $box (6, 10, 6)
// test.kt:51 $box$lambda.invoke (8)
// test.kt:54 $box$lambda.invoke (10, 14, 10)
// test.kt:53 $box$lambda$lambda.invoke (12, 19)
// test.kt:54 $box$lambda.invoke (17)
// test.kt:55 $box (6)

// test.kt:58 $box (4)
// test.kt:67 $box (9, 13, 9)
// test.kt:61 $box$lambda.invoke (8)
// test.kt:65 $box$lambda.invoke (13, 17, 13)
// test.kt:63 $box$lambda$lambda.invoke (12, 19)
// test.kt:65 $box$lambda.invoke (20)
// test.kt:67 $box (9)

// test.kt:69 $box (11, 4)

// EXPECTATIONS JS_IR
// test.kt:9 box
// test.kt:9 box
// test.kt:9 <init>
// test.kt:9 <init>
// test.kt:9 box
// test.kt:9 box$lambda
// test.kt:9 box$lambda
// test.kt:9 <init>
// test.kt:9 <init>
// test.kt:9 box$lambda
// test.kt:9 box$lambda$lambda

// test.kt:11 box
// test.kt:13 box
// test.kt:9 <init>
// test.kt:9 <init>
// test.kt:13 box
// test.kt:11 box$lambda
// test.kt:12 box$lambda
// test.kt:9 <init>
// test.kt:9 <init>
// test.kt:12 box$lambda
// test.kt:11 box$lambda$lambda
// test.kt:15 box

// test.kt:19 box
// test.kt:9 <init>
// test.kt:9 <init>
// test.kt:19 box
// test.kt:16 box$lambda
// test.kt:18 box$lambda
// test.kt:9 <init>
// test.kt:9 <init>
// test.kt:18 box$lambda
// test.kt:17 box$lambda$lambda
// test.kt:21 box

// test.kt:27 box
// test.kt:9 <init>
// test.kt:9 <init>
// test.kt:27 box
// test.kt:22 box$lambda
// test.kt:25 box$lambda
// test.kt:9 <init>
// test.kt:9 <init>
// test.kt:25 box$lambda
// test.kt:23 box$lambda$lambda
// test.kt:29 box

// test.kt:35 box
// test.kt:9 <init>
// test.kt:9 <init>
// test.kt:35 box
// test.kt:31 box$lambda
// test.kt:34 box$lambda
// test.kt:9 <init>
// test.kt:9 <init>
// test.kt:34 box$lambda
// test.kt:33 box$lambda$lambda
// test.kt:37 box

// test.kt:45 box
// test.kt:9 <init>
// test.kt:9 <init>
// test.kt:45 box
// test.kt:39 box$lambda
// test.kt:43 box$lambda
// test.kt:9 <init>
// test.kt:9 <init>
// test.kt:43 box$lambda
// test.kt:41 box$lambda$lambda
// test.kt:48 box

// test.kt:55 box
// test.kt:9 <init>
// test.kt:9 <init>
// test.kt:55 box
// test.kt:51 box$lambda
// test.kt:54 box$lambda
// test.kt:9 <init>
// test.kt:9 <init>
// test.kt:54 box$lambda
// test.kt:53 box$lambda$lambda
// test.kt:58 box

// test.kt:67 box
// test.kt:9 <init>
// test.kt:9 <init>
// test.kt:67 box
// test.kt:61 box$lambda
// test.kt:65 box$lambda
// test.kt:9 <init>
// test.kt:9 <init>
// test.kt:65 box$lambda
// test.kt:63 box$lambda$lambda

// test.kt:69 box

// EXPECTATIONS NATIVE
// test.kt:9 box
// test.kt:9 bar
// test.kt:9 bar
// test.kt:9 bar
// test.kt:9 box

// test.kt:13 box
// test.kt:11 bar
// test.kt:12 bar
// test.kt:11 bar
// test.kt:12 bar
// test.kt:13 box

// test.kt:19 box
// test.kt:15 bar
// test.kt:18 bar
// test.kt:16 bar
// test.kt:17 bar
// test.kt:18 bar
// test.kt:18 bar
// test.kt:19 bar
// test.kt:19 box

// test.kt:27 box
// test.kt:21 bar
// test.kt:25 bar
// test.kt:22 bar
// test.kt:23 bar
// test.kt:24 bar
// test.kt:25 bar
// test.kt:26 bar
// test.kt:27 box

// test.kt:35 box
// test.kt:29 bar
// test.kt:34 bar
// test.kt:31 bar
// test.kt:33 bar
// test.kt:34 bar
// test.kt:34 bar
// test.kt:35 bar
// test.kt:35 box

// test.kt:45 box
// test.kt:37 bar
// test.kt:43 bar
// test.kt:39 bar
// test.kt:41 bar
// test.kt:42 bar
// test.kt:43 bar
// test.kt:44 bar
// test.kt:45 box

// test.kt:55 box
// test.kt:48 bar
// test.kt:54 bar
// test.kt:51 bar
// test.kt:53 bar
// test.kt:54 bar
// test.kt:54 bar
// test.kt:55 bar
// test.kt:55 box

// test.kt:67 box
// test.kt:58 bar
// test.kt:65 bar
// test.kt:61 bar
// test.kt:63 bar
// test.kt:64 bar
// test.kt:65 bar
// test.kt:66 bar
// test.kt:67 box

// test.kt:69 box
// test.kt:70 box
