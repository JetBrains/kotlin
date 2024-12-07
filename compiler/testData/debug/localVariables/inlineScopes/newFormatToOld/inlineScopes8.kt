// WITH_STDLIB
// USE_INLINE_SCOPES_NUMBERS
// MODULE: library
// FILE: library.kt

class MyClass {
    inline fun f1(f1Param: () -> Unit): MyClass {
        test()
        f1Param()
        return this
    }

    inline fun f2(f1Param: () -> Unit): MyClass {
        test()
        f1Param()
        return this
    }
}

inline fun foo() {
    val array = arrayOf(1, 2)
    val myClass = MyClass()

    test()
    myClass.f1 { test() }
        .f2 { test() }

    test()
    myClass.f1 { test() }
        .f2 { test() }

    test()
    array.map {
        it * 2
    }

    test()
    array.map { it * 2 }
        .filter {
            it > 2
        }

    test()
    array.map { it * 2 }
        .filter {
            it > 2
        }

    test()
    myClass.f1 { test() }.f2 { test() }

    test()
    myClass.f1 { test() }.f2 { test() }
}

inline fun test() {
    val testVal = 1
}

// MODULE: test(library)
// FILE: test.kt

fun box() {
    foo()
    foo()
}

// EXPECTATIONS JVM_IR
// test.kt:64 box:
// library.kt:21 box: $i$f$foo\1\64:int=0:int
// library.kt:22 box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[]
// library.kt:6 <init>:
// library.kt:22 box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[]
// library.kt:24 box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass
// library.kt:57 box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass, $i$f$test\2\418:int=0:int
// library.kt:58 box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass, $i$f$test\2\418:int=0:int, testVal\2:int=1:int
// library.kt:25 box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass
// library.kt:8 box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass, this_\3:MyClass=MyClass, $i$f$f1\3\421:int=0:int
// library.kt:57 box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass, this_\3:MyClass=MyClass, $i$f$f1\3\421:int=0:int, $i$f$test\4\422:int=0:int
// library.kt:58 box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass, this_\3:MyClass=MyClass, $i$f$f1\3\421:int=0:int, $i$f$test\4\422:int=0:int, testVal\4:int=1:int
// library.kt:9 box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass, this_\3:MyClass=MyClass, $i$f$f1\3\421:int=0:int
// library.kt:25 box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass, this_\3:MyClass=MyClass, $i$f$f1\3\421:int=0:int, $i$a$-f1-LibraryKt$foo$1\5\425\1:int=0:int
// library.kt:57 box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass, this_\3:MyClass=MyClass, $i$f$f1\3\421:int=0:int, $i$a$-f1-LibraryKt$foo$1\5\425\1:int=0:int, $i$f$test\6\421:int=0:int
// library.kt:58 box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass, this_\3:MyClass=MyClass, $i$f$f1\3\421:int=0:int, $i$a$-f1-LibraryKt$foo$1\5\425\1:int=0:int, $i$f$test\6\421:int=0:int, testVal\6:int=1:int
// library.kt:25 box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass, this_\3:MyClass=MyClass, $i$f$f1\3\421:int=0:int, $i$a$-f1-LibraryKt$foo$1\5\425\1:int=0:int
// library.kt:9 box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass, this_\3:MyClass=MyClass, $i$f$f1\3\421:int=0:int
// library.kt:10 box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass, this_\3:MyClass=MyClass, $i$f$f1\3\421:int=0:int
// library.kt:26 box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass
// library.kt:14 box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass, this_\7:MyClass=MyClass, $i$f$f2\7\427:int=0:int
// library.kt:57 box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass, this_\7:MyClass=MyClass, $i$f$f2\7\427:int=0:int, $i$f$test\8\428:int=0:int
// library.kt:58 box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass, this_\7:MyClass=MyClass, $i$f$f2\7\427:int=0:int, $i$f$test\8\428:int=0:int, testVal\8:int=1:int
// library.kt:15 box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass, this_\7:MyClass=MyClass, $i$f$f2\7\427:int=0:int
// library.kt:26 box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass, this_\7:MyClass=MyClass, $i$f$f2\7\427:int=0:int, $i$a$-f2-LibraryKt$foo$2\9\431\1:int=0:int
// library.kt:57 box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass, this_\7:MyClass=MyClass, $i$f$f2\7\427:int=0:int, $i$a$-f2-LibraryKt$foo$2\9\431\1:int=0:int, $i$f$test\10\427:int=0:int
// library.kt:58 box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass, this_\7:MyClass=MyClass, $i$f$f2\7\427:int=0:int, $i$a$-f2-LibraryKt$foo$2\9\431\1:int=0:int, $i$f$test\10\427:int=0:int, testVal\10:int=1:int
// library.kt:26 box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass, this_\7:MyClass=MyClass, $i$f$f2\7\427:int=0:int, $i$a$-f2-LibraryKt$foo$2\9\431\1:int=0:int
// library.kt:15 box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass, this_\7:MyClass=MyClass, $i$f$f2\7\427:int=0:int
// library.kt:16 box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass, this_\7:MyClass=MyClass, $i$f$f2\7\427:int=0:int
// library.kt:28 box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass
// library.kt:57 box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass, $i$f$test\11\433:int=0:int
// library.kt:58 box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass, $i$f$test\11\433:int=0:int, testVal\11:int=1:int
// library.kt:29 box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass
// library.kt:8 box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass, this_\12:MyClass=MyClass, $i$f$f1\12\436:int=0:int
// library.kt:57 box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass, this_\12:MyClass=MyClass, $i$f$f1\12\436:int=0:int, $i$f$test\13\422:int=0:int
// library.kt:58 box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass, this_\12:MyClass=MyClass, $i$f$f1\12\436:int=0:int, $i$f$test\13\422:int=0:int, testVal\13:int=1:int
// library.kt:9 box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass, this_\12:MyClass=MyClass, $i$f$f1\12\436:int=0:int
// library.kt:29 box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass, this_\12:MyClass=MyClass, $i$f$f1\12\436:int=0:int, $i$a$-f1-LibraryKt$foo$3\14\439\1:int=0:int
// library.kt:57 box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass, this_\12:MyClass=MyClass, $i$f$f1\12\436:int=0:int, $i$a$-f1-LibraryKt$foo$3\14\439\1:int=0:int, $i$f$test\15\436:int=0:int
// library.kt:58 box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass, this_\12:MyClass=MyClass, $i$f$f1\12\436:int=0:int, $i$a$-f1-LibraryKt$foo$3\14\439\1:int=0:int, $i$f$test\15\436:int=0:int, testVal\15:int=1:int
// library.kt:29 box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass, this_\12:MyClass=MyClass, $i$f$f1\12\436:int=0:int, $i$a$-f1-LibraryKt$foo$3\14\439\1:int=0:int
// library.kt:9 box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass, this_\12:MyClass=MyClass, $i$f$f1\12\436:int=0:int
// library.kt:10 box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass, this_\12:MyClass=MyClass, $i$f$f1\12\436:int=0:int
// library.kt:30 box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass
// library.kt:14 box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass, this_\16:MyClass=MyClass, $i$f$f2\16\441:int=0:int
// library.kt:57 box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass, this_\16:MyClass=MyClass, $i$f$f2\16\441:int=0:int, $i$f$test\17\442:int=0:int
// library.kt:58 box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass, this_\16:MyClass=MyClass, $i$f$f2\16\441:int=0:int, $i$f$test\17\442:int=0:int, testVal\17:int=1:int
// library.kt:15 box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass, this_\16:MyClass=MyClass, $i$f$f2\16\441:int=0:int
// library.kt:30 box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass, this_\16:MyClass=MyClass, $i$f$f2\16\441:int=0:int, $i$a$-f2-LibraryKt$foo$4\18\445\1:int=0:int
// library.kt:57 box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass, this_\16:MyClass=MyClass, $i$f$f2\16\441:int=0:int, $i$a$-f2-LibraryKt$foo$4\18\445\1:int=0:int, $i$f$test\19\441:int=0:int
// library.kt:58 box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass, this_\16:MyClass=MyClass, $i$f$f2\16\441:int=0:int, $i$a$-f2-LibraryKt$foo$4\18\445\1:int=0:int, $i$f$test\19\441:int=0:int, testVal\19:int=1:int
// library.kt:30 box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass, this_\16:MyClass=MyClass, $i$f$f2\16\441:int=0:int, $i$a$-f2-LibraryKt$foo$4\18\445\1:int=0:int
// library.kt:15 box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass, this_\16:MyClass=MyClass, $i$f$f2\16\441:int=0:int
// library.kt:16 box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass, this_\16:MyClass=MyClass, $i$f$f2\16\441:int=0:int
// library.kt:32 box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass
// library.kt:57 box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass, $i$f$test\20\447:int=0:int
// library.kt:58 box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass, $i$f$test\20\447:int=0:int, testVal\20:int=1:int
// library.kt:33 box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass
// _Arrays.kt:... box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass, $this$map\21:java.lang.Object[]=java.lang.Integer[], $i$f$map\21\450:int=0:int
// _Arrays.kt:... box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass, $this$map\21:java.lang.Object[]=java.lang.Integer[], $i$f$map\21\450:int=0:int, $this$mapTo\22:java.lang.Object[]=java.lang.Integer[], destination\22:java.util.Collection=java.util.ArrayList, $i$f$mapTo\22\451:int=0:int
// _Arrays.kt:... box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass, $this$map\21:java.lang.Object[]=java.lang.Integer[], $i$f$map\21\450:int=0:int, $this$mapTo\22:java.lang.Object[]=java.lang.Integer[], destination\22:java.util.Collection=java.util.ArrayList, $i$f$mapTo\22\451:int=0:int, item\22:java.lang.Object=java.lang.Integer
// library.kt:34 box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass, $this$map\21:java.lang.Object[]=java.lang.Integer[], $i$f$map\21\450:int=0:int, $this$mapTo\22:java.lang.Object[]=java.lang.Integer[], destination\22:java.util.Collection=java.util.ArrayList, $i$f$mapTo\22\451:int=0:int, item\22:java.lang.Object=java.lang.Integer, it\23:int=1:int, $i$a$-map-LibraryKt$foo$5\23\453\1:int=0:int
// _Arrays.kt:... box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass, $this$map\21:java.lang.Object[]=java.lang.Integer[], $i$f$map\21\450:int=0:int, $this$mapTo\22:java.lang.Object[]=java.lang.Integer[], destination\22:java.util.Collection=java.util.ArrayList, $i$f$mapTo\22\451:int=0:int, item\22:java.lang.Object=java.lang.Integer
// _Arrays.kt:... box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass, $this$map\21:java.lang.Object[]=java.lang.Integer[], $i$f$map\21\450:int=0:int, $this$mapTo\22:java.lang.Object[]=java.lang.Integer[], destination\22:java.util.Collection=java.util.ArrayList, $i$f$mapTo\22\451:int=0:int
// _Arrays.kt:... box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass, $this$map\21:java.lang.Object[]=java.lang.Integer[], $i$f$map\21\450:int=0:int, $this$mapTo\22:java.lang.Object[]=java.lang.Integer[], destination\22:java.util.Collection=java.util.ArrayList, $i$f$mapTo\22\451:int=0:int, item\22:java.lang.Object=java.lang.Integer
// library.kt:34 box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass, $this$map\21:java.lang.Object[]=java.lang.Integer[], $i$f$map\21\450:int=0:int, $this$mapTo\22:java.lang.Object[]=java.lang.Integer[], destination\22:java.util.Collection=java.util.ArrayList, $i$f$mapTo\22\451:int=0:int, item\22:java.lang.Object=java.lang.Integer, it\23:int=2:int, $i$a$-map-LibraryKt$foo$5\23\453\1:int=0:int
// _Arrays.kt:... box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass, $this$map\21:java.lang.Object[]=java.lang.Integer[], $i$f$map\21\450:int=0:int, $this$mapTo\22:java.lang.Object[]=java.lang.Integer[], destination\22:java.util.Collection=java.util.ArrayList, $i$f$mapTo\22\451:int=0:int, item\22:java.lang.Object=java.lang.Integer
// _Arrays.kt:... box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass, $this$map\21:java.lang.Object[]=java.lang.Integer[], $i$f$map\21\450:int=0:int, $this$mapTo\22:java.lang.Object[]=java.lang.Integer[], destination\22:java.util.Collection=java.util.ArrayList, $i$f$mapTo\22\451:int=0:int
// _Arrays.kt:... box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass, $this$map\21:java.lang.Object[]=java.lang.Integer[], $i$f$map\21\450:int=0:int, $this$mapTo\22:java.lang.Object[]=java.lang.Integer[], destination\22:java.util.Collection=java.util.ArrayList, $i$f$mapTo\22\451:int=0:int
// _Arrays.kt:... box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass, $this$map\21:java.lang.Object[]=java.lang.Integer[], $i$f$map\21\450:int=0:int
// library.kt:37 box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass
// library.kt:57 box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass, $i$f$test\24\456:int=0:int
// library.kt:58 box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass, $i$f$test\24\456:int=0:int, testVal\24:int=1:int
// library.kt:38 box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass
// _Arrays.kt:... box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass, $this$map\25:java.lang.Object[]=java.lang.Integer[], $i$f$map\25\459:int=0:int
// _Arrays.kt:... box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass, $this$map\25:java.lang.Object[]=java.lang.Integer[], $i$f$map\25\459:int=0:int, $this$mapTo\26:java.lang.Object[]=java.lang.Integer[], destination\26:java.util.Collection=java.util.ArrayList, $i$f$mapTo\26\451:int=0:int
// _Arrays.kt:... box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass, $this$map\25:java.lang.Object[]=java.lang.Integer[], $i$f$map\25\459:int=0:int, $this$mapTo\26:java.lang.Object[]=java.lang.Integer[], destination\26:java.util.Collection=java.util.ArrayList, $i$f$mapTo\26\451:int=0:int, item\26:java.lang.Object=java.lang.Integer
// library.kt:38 box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass, $this$map\25:java.lang.Object[]=java.lang.Integer[], $i$f$map\25\459:int=0:int, $this$mapTo\26:java.lang.Object[]=java.lang.Integer[], destination\26:java.util.Collection=java.util.ArrayList, $i$f$mapTo\26\451:int=0:int, item\26:java.lang.Object=java.lang.Integer, it\27:int=1:int, $i$a$-map-LibraryKt$foo$6\27\461\1:int=0:int
// _Arrays.kt:... box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass, $this$map\25:java.lang.Object[]=java.lang.Integer[], $i$f$map\25\459:int=0:int, $this$mapTo\26:java.lang.Object[]=java.lang.Integer[], destination\26:java.util.Collection=java.util.ArrayList, $i$f$mapTo\26\451:int=0:int, item\26:java.lang.Object=java.lang.Integer
// _Arrays.kt:... box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass, $this$map\25:java.lang.Object[]=java.lang.Integer[], $i$f$map\25\459:int=0:int, $this$mapTo\26:java.lang.Object[]=java.lang.Integer[], destination\26:java.util.Collection=java.util.ArrayList, $i$f$mapTo\26\451:int=0:int
// _Arrays.kt:... box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass, $this$map\25:java.lang.Object[]=java.lang.Integer[], $i$f$map\25\459:int=0:int, $this$mapTo\26:java.lang.Object[]=java.lang.Integer[], destination\26:java.util.Collection=java.util.ArrayList, $i$f$mapTo\26\451:int=0:int, item\26:java.lang.Object=java.lang.Integer
// library.kt:38 box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass, $this$map\25:java.lang.Object[]=java.lang.Integer[], $i$f$map\25\459:int=0:int, $this$mapTo\26:java.lang.Object[]=java.lang.Integer[], destination\26:java.util.Collection=java.util.ArrayList, $i$f$mapTo\26\451:int=0:int, item\26:java.lang.Object=java.lang.Integer, it\27:int=2:int, $i$a$-map-LibraryKt$foo$6\27\461\1:int=0:int
// _Arrays.kt:... box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass, $this$map\25:java.lang.Object[]=java.lang.Integer[], $i$f$map\25\459:int=0:int, $this$mapTo\26:java.lang.Object[]=java.lang.Integer[], destination\26:java.util.Collection=java.util.ArrayList, $i$f$mapTo\26\451:int=0:int, item\26:java.lang.Object=java.lang.Integer
// _Arrays.kt:... box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass, $this$map\25:java.lang.Object[]=java.lang.Integer[], $i$f$map\25\459:int=0:int, $this$mapTo\26:java.lang.Object[]=java.lang.Integer[], destination\26:java.util.Collection=java.util.ArrayList, $i$f$mapTo\26\451:int=0:int
// _Arrays.kt:... box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass, $this$map\25:java.lang.Object[]=java.lang.Integer[], $i$f$map\25\459:int=0:int, $this$mapTo\26:java.lang.Object[]=java.lang.Integer[], destination\26:java.util.Collection=java.util.ArrayList, $i$f$mapTo\26\451:int=0:int
// _Arrays.kt:... box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass, $this$map\25:java.lang.Object[]=java.lang.Integer[], $i$f$map\25\459:int=0:int
// library.kt:39 box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass
// _Collections.kt:... box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass, $this$filter\28:java.lang.Iterable=java.util.ArrayList, $i$f$filter\28\463:int=0:int
// _Collections.kt:... box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass, $this$filter\28:java.lang.Iterable=java.util.ArrayList, $i$f$filter\28\463:int=0:int, $this$filterTo\29:java.lang.Iterable=java.util.ArrayList, destination\29:java.util.Collection=java.util.ArrayList, $i$f$filterTo\29\464:int=0:int
// library.kt:40 box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass, $this$filter\28:java.lang.Iterable=java.util.ArrayList, $i$f$filter\28\463:int=0:int, $this$filterTo\29:java.lang.Iterable=java.util.ArrayList, destination\29:java.util.Collection=java.util.ArrayList, $i$f$filterTo\29\464:int=0:int, element\29:java.lang.Object=java.lang.Integer, it\30:int=2:int, $i$a$-filter-LibraryKt$foo$7\30\465\1:int=0:int
// _Collections.kt:... box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass, $this$filter\28:java.lang.Iterable=java.util.ArrayList, $i$f$filter\28\463:int=0:int, $this$filterTo\29:java.lang.Iterable=java.util.ArrayList, destination\29:java.util.Collection=java.util.ArrayList, $i$f$filterTo\29\464:int=0:int, element\29:java.lang.Object=java.lang.Integer
// library.kt:40 box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass, $this$filter\28:java.lang.Iterable=java.util.ArrayList, $i$f$filter\28\463:int=0:int, $this$filterTo\29:java.lang.Iterable=java.util.ArrayList, destination\29:java.util.Collection=java.util.ArrayList, $i$f$filterTo\29\464:int=0:int, element\29:java.lang.Object=java.lang.Integer, it\30:int=4:int, $i$a$-filter-LibraryKt$foo$7\30\465\1:int=0:int
// _Collections.kt:... box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass, $this$filter\28:java.lang.Iterable=java.util.ArrayList, $i$f$filter\28\463:int=0:int, $this$filterTo\29:java.lang.Iterable=java.util.ArrayList, destination\29:java.util.Collection=java.util.ArrayList, $i$f$filterTo\29\464:int=0:int, element\29:java.lang.Object=java.lang.Integer
// _Collections.kt:... box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass, $this$filter\28:java.lang.Iterable=java.util.ArrayList, $i$f$filter\28\463:int=0:int, $this$filterTo\29:java.lang.Iterable=java.util.ArrayList, destination\29:java.util.Collection=java.util.ArrayList, $i$f$filterTo\29\464:int=0:int
// _Collections.kt:... box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass, $this$filter\28:java.lang.Iterable=java.util.ArrayList, $i$f$filter\28\463:int=0:int
// library.kt:43 box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass
// library.kt:57 box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass, $i$f$test\31\468:int=0:int
// library.kt:58 box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass, $i$f$test\31\468:int=0:int, testVal\31:int=1:int
// library.kt:44 box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass
// _Arrays.kt:... box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass, $this$map\32:java.lang.Object[]=java.lang.Integer[], $i$f$map\32\471:int=0:int
// _Arrays.kt:... box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass, $this$map\32:java.lang.Object[]=java.lang.Integer[], $i$f$map\32\471:int=0:int, $this$mapTo\33:java.lang.Object[]=java.lang.Integer[], destination\33:java.util.Collection=java.util.ArrayList, $i$f$mapTo\33\451:int=0:int
// _Arrays.kt:... box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass, $this$map\32:java.lang.Object[]=java.lang.Integer[], $i$f$map\32\471:int=0:int, $this$mapTo\33:java.lang.Object[]=java.lang.Integer[], destination\33:java.util.Collection=java.util.ArrayList, $i$f$mapTo\33\451:int=0:int, item\33:java.lang.Object=java.lang.Integer
// library.kt:44 box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass, $this$map\32:java.lang.Object[]=java.lang.Integer[], $i$f$map\32\471:int=0:int, $this$mapTo\33:java.lang.Object[]=java.lang.Integer[], destination\33:java.util.Collection=java.util.ArrayList, $i$f$mapTo\33\451:int=0:int, item\33:java.lang.Object=java.lang.Integer, it\34:int=1:int, $i$a$-map-LibraryKt$foo$8\34\461\1:int=0:int
// _Arrays.kt:... box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass, $this$map\32:java.lang.Object[]=java.lang.Integer[], $i$f$map\32\471:int=0:int, $this$mapTo\33:java.lang.Object[]=java.lang.Integer[], destination\33:java.util.Collection=java.util.ArrayList, $i$f$mapTo\33\451:int=0:int, item\33:java.lang.Object=java.lang.Integer
// _Arrays.kt:... box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass, $this$map\32:java.lang.Object[]=java.lang.Integer[], $i$f$map\32\471:int=0:int, $this$mapTo\33:java.lang.Object[]=java.lang.Integer[], destination\33:java.util.Collection=java.util.ArrayList, $i$f$mapTo\33\451:int=0:int
// _Arrays.kt:... box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass, $this$map\32:java.lang.Object[]=java.lang.Integer[], $i$f$map\32\471:int=0:int, $this$mapTo\33:java.lang.Object[]=java.lang.Integer[], destination\33:java.util.Collection=java.util.ArrayList, $i$f$mapTo\33\451:int=0:int, item\33:java.lang.Object=java.lang.Integer
// library.kt:44 box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass, $this$map\32:java.lang.Object[]=java.lang.Integer[], $i$f$map\32\471:int=0:int, $this$mapTo\33:java.lang.Object[]=java.lang.Integer[], destination\33:java.util.Collection=java.util.ArrayList, $i$f$mapTo\33\451:int=0:int, item\33:java.lang.Object=java.lang.Integer, it\34:int=2:int, $i$a$-map-LibraryKt$foo$8\34\461\1:int=0:int
// _Arrays.kt:... box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass, $this$map\32:java.lang.Object[]=java.lang.Integer[], $i$f$map\32\471:int=0:int, $this$mapTo\33:java.lang.Object[]=java.lang.Integer[], destination\33:java.util.Collection=java.util.ArrayList, $i$f$mapTo\33\451:int=0:int, item\33:java.lang.Object=java.lang.Integer
// _Arrays.kt:... box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass, $this$map\32:java.lang.Object[]=java.lang.Integer[], $i$f$map\32\471:int=0:int, $this$mapTo\33:java.lang.Object[]=java.lang.Integer[], destination\33:java.util.Collection=java.util.ArrayList, $i$f$mapTo\33\451:int=0:int
// _Arrays.kt:... box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass, $this$map\32:java.lang.Object[]=java.lang.Integer[], $i$f$map\32\471:int=0:int, $this$mapTo\33:java.lang.Object[]=java.lang.Integer[], destination\33:java.util.Collection=java.util.ArrayList, $i$f$mapTo\33\451:int=0:int
// _Arrays.kt:... box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass, $this$map\32:java.lang.Object[]=java.lang.Integer[], $i$f$map\32\471:int=0:int
// library.kt:45 box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass
// _Collections.kt:... box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass, $this$filter\35:java.lang.Iterable=java.util.ArrayList, $i$f$filter\35\472:int=0:int
// _Collections.kt:... box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass, $this$filter\35:java.lang.Iterable=java.util.ArrayList, $i$f$filter\35\472:int=0:int, $this$filterTo\36:java.lang.Iterable=java.util.ArrayList, destination\36:java.util.Collection=java.util.ArrayList, $i$f$filterTo\36\464:int=0:int
// library.kt:46 box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass, $this$filter\35:java.lang.Iterable=java.util.ArrayList, $i$f$filter\35\472:int=0:int, $this$filterTo\36:java.lang.Iterable=java.util.ArrayList, destination\36:java.util.Collection=java.util.ArrayList, $i$f$filterTo\36\464:int=0:int, element\36:java.lang.Object=java.lang.Integer, it\37:int=2:int, $i$a$-filter-LibraryKt$foo$9\37\473\1:int=0:int
// _Collections.kt:... box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass, $this$filter\35:java.lang.Iterable=java.util.ArrayList, $i$f$filter\35\472:int=0:int, $this$filterTo\36:java.lang.Iterable=java.util.ArrayList, destination\36:java.util.Collection=java.util.ArrayList, $i$f$filterTo\36\464:int=0:int, element\36:java.lang.Object=java.lang.Integer
// library.kt:46 box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass, $this$filter\35:java.lang.Iterable=java.util.ArrayList, $i$f$filter\35\472:int=0:int, $this$filterTo\36:java.lang.Iterable=java.util.ArrayList, destination\36:java.util.Collection=java.util.ArrayList, $i$f$filterTo\36\464:int=0:int, element\36:java.lang.Object=java.lang.Integer, it\37:int=4:int, $i$a$-filter-LibraryKt$foo$9\37\473\1:int=0:int
// _Collections.kt:... box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass, $this$filter\35:java.lang.Iterable=java.util.ArrayList, $i$f$filter\35\472:int=0:int, $this$filterTo\36:java.lang.Iterable=java.util.ArrayList, destination\36:java.util.Collection=java.util.ArrayList, $i$f$filterTo\36\464:int=0:int, element\36:java.lang.Object=java.lang.Integer
// _Collections.kt:... box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass, $this$filter\35:java.lang.Iterable=java.util.ArrayList, $i$f$filter\35\472:int=0:int, $this$filterTo\36:java.lang.Iterable=java.util.ArrayList, destination\36:java.util.Collection=java.util.ArrayList, $i$f$filterTo\36\464:int=0:int
// _Collections.kt:... box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass, $this$filter\35:java.lang.Iterable=java.util.ArrayList, $i$f$filter\35\472:int=0:int
// library.kt:49 box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass
// library.kt:57 box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass, $i$f$test\38\476:int=0:int
// library.kt:58 box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass, $i$f$test\38\476:int=0:int, testVal\38:int=1:int
// library.kt:50 box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass
// library.kt:8 box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass, this_\39:MyClass=MyClass, $i$f$f1\39\477:int=0:int
// library.kt:57 box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass, this_\39:MyClass=MyClass, $i$f$f1\39\477:int=0:int, $i$f$test\40\422:int=0:int
// library.kt:58 box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass, this_\39:MyClass=MyClass, $i$f$f1\39\477:int=0:int, $i$f$test\40\422:int=0:int, testVal\40:int=1:int
// library.kt:9 box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass, this_\39:MyClass=MyClass, $i$f$f1\39\477:int=0:int
// library.kt:50 box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass, this_\39:MyClass=MyClass, $i$f$f1\39\477:int=0:int, $i$a$-f1-LibraryKt$foo$10\41\486\1:int=0:int
// library.kt:57 box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass, this_\39:MyClass=MyClass, $i$f$f1\39\477:int=0:int, $i$a$-f1-LibraryKt$foo$10\41\486\1:int=0:int, $i$f$test\42\477:int=0:int
// library.kt:58 box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass, this_\39:MyClass=MyClass, $i$f$f1\39\477:int=0:int, $i$a$-f1-LibraryKt$foo$10\41\486\1:int=0:int, $i$f$test\42\477:int=0:int, testVal\42:int=1:int
// library.kt:50 box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass, this_\39:MyClass=MyClass, $i$f$f1\39\477:int=0:int, $i$a$-f1-LibraryKt$foo$10\41\486\1:int=0:int
// library.kt:9 box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass, this_\39:MyClass=MyClass, $i$f$f1\39\477:int=0:int
// library.kt:10 box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass, this_\39:MyClass=MyClass, $i$f$f1\39\477:int=0:int
// library.kt:50 box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass
// library.kt:14 box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass, this_\43:MyClass=MyClass, $i$f$f2\43\477:int=0:int
// library.kt:57 box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass, this_\43:MyClass=MyClass, $i$f$f2\43\477:int=0:int, $i$f$test\44\491:int=0:int
// library.kt:58 box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass, this_\43:MyClass=MyClass, $i$f$f2\43\477:int=0:int, $i$f$test\44\491:int=0:int, testVal\44:int=1:int
// library.kt:15 box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass, this_\43:MyClass=MyClass, $i$f$f2\43\477:int=0:int
// library.kt:50 box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass, this_\43:MyClass=MyClass, $i$f$f2\43\477:int=0:int, $i$a$-f2-LibraryKt$foo$11\45\492\1:int=0:int
// library.kt:57 box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass, this_\43:MyClass=MyClass, $i$f$f2\43\477:int=0:int, $i$a$-f2-LibraryKt$foo$11\45\492\1:int=0:int, $i$f$test\46\477:int=0:int
// library.kt:58 box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass, this_\43:MyClass=MyClass, $i$f$f2\43\477:int=0:int, $i$a$-f2-LibraryKt$foo$11\45\492\1:int=0:int, $i$f$test\46\477:int=0:int, testVal\46:int=1:int
// library.kt:50 box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass, this_\43:MyClass=MyClass, $i$f$f2\43\477:int=0:int, $i$a$-f2-LibraryKt$foo$11\45\492\1:int=0:int
// library.kt:15 box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass, this_\43:MyClass=MyClass, $i$f$f2\43\477:int=0:int
// library.kt:16 box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass, this_\43:MyClass=MyClass, $i$f$f2\43\477:int=0:int
// library.kt:52 box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass
// library.kt:57 box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass, $i$f$test\47\479:int=0:int
// library.kt:58 box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass, $i$f$test\47\479:int=0:int, testVal\47:int=1:int
// library.kt:53 box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass
// library.kt:8 box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass, this_\48:MyClass=MyClass, $i$f$f1\48\480:int=0:int
// library.kt:57 box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass, this_\48:MyClass=MyClass, $i$f$f1\48\480:int=0:int, $i$f$test\49\422:int=0:int
// library.kt:58 box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass, this_\48:MyClass=MyClass, $i$f$f1\48\480:int=0:int, $i$f$test\49\422:int=0:int, testVal\49:int=1:int
// library.kt:9 box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass, this_\48:MyClass=MyClass, $i$f$f1\48\480:int=0:int
// library.kt:53 box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass, this_\48:MyClass=MyClass, $i$f$f1\48\480:int=0:int, $i$a$-f1-LibraryKt$foo$12\50\486\1:int=0:int
// library.kt:57 box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass, this_\48:MyClass=MyClass, $i$f$f1\48\480:int=0:int, $i$a$-f1-LibraryKt$foo$12\50\486\1:int=0:int, $i$f$test\51\480:int=0:int
// library.kt:58 box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass, this_\48:MyClass=MyClass, $i$f$f1\48\480:int=0:int, $i$a$-f1-LibraryKt$foo$12\50\486\1:int=0:int, $i$f$test\51\480:int=0:int, testVal\51:int=1:int
// library.kt:53 box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass, this_\48:MyClass=MyClass, $i$f$f1\48\480:int=0:int, $i$a$-f1-LibraryKt$foo$12\50\486\1:int=0:int
// library.kt:9 box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass, this_\48:MyClass=MyClass, $i$f$f1\48\480:int=0:int
// library.kt:10 box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass, this_\48:MyClass=MyClass, $i$f$f1\48\480:int=0:int
// library.kt:53 box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass
// library.kt:14 box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass, this_\52:MyClass=MyClass, $i$f$f2\52\480:int=0:int
// library.kt:57 box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass, this_\52:MyClass=MyClass, $i$f$f2\52\480:int=0:int, $i$f$test\53\491:int=0:int
// library.kt:58 box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass, this_\52:MyClass=MyClass, $i$f$f2\52\480:int=0:int, $i$f$test\53\491:int=0:int, testVal\53:int=1:int
// library.kt:15 box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass, this_\52:MyClass=MyClass, $i$f$f2\52\480:int=0:int
// library.kt:53 box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass, this_\52:MyClass=MyClass, $i$f$f2\52\480:int=0:int, $i$a$-f2-LibraryKt$foo$13\54\492\1:int=0:int
// library.kt:57 box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass, this_\52:MyClass=MyClass, $i$f$f2\52\480:int=0:int, $i$a$-f2-LibraryKt$foo$13\54\492\1:int=0:int, $i$f$test\55\480:int=0:int
// library.kt:58 box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass, this_\52:MyClass=MyClass, $i$f$f2\52\480:int=0:int, $i$a$-f2-LibraryKt$foo$13\54\492\1:int=0:int, $i$f$test\55\480:int=0:int, testVal\55:int=1:int
// library.kt:53 box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass, this_\52:MyClass=MyClass, $i$f$f2\52\480:int=0:int, $i$a$-f2-LibraryKt$foo$13\54\492\1:int=0:int
// library.kt:15 box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass, this_\52:MyClass=MyClass, $i$f$f2\52\480:int=0:int
// library.kt:16 box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass, this_\52:MyClass=MyClass, $i$f$f2\52\480:int=0:int
// library.kt:54 box: $i$f$foo\1\64:int=0:int, array\1:java.lang.Integer[]=java.lang.Integer[], myClass\1:MyClass=MyClass
// test.kt:65 box:
// library.kt:21 box: $i$f$foo\56\65:int=0:int
// library.kt:22 box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[]
// library.kt:6 <init>:
// library.kt:22 box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[]
// library.kt:24 box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass
// library.kt:57 box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass, $i$f$test\57\497:int=0:int
// library.kt:58 box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass, $i$f$test\57\497:int=0:int, testVal\57:int=1:int
// library.kt:25 box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass
// library.kt:8 box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass, this_\58:MyClass=MyClass, $i$f$f1\58\500:int=0:int
// library.kt:57 box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass, this_\58:MyClass=MyClass, $i$f$f1\58\500:int=0:int, $i$f$test\59\501:int=0:int
// library.kt:58 box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass, this_\58:MyClass=MyClass, $i$f$f1\58\500:int=0:int, $i$f$test\59\501:int=0:int, testVal\59:int=1:int
// library.kt:9 box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass, this_\58:MyClass=MyClass, $i$f$f1\58\500:int=0:int
// library.kt:25 box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass, this_\58:MyClass=MyClass, $i$f$f1\58\500:int=0:int, $i$a$-f1-LibraryKt$foo$1\60\504\56:int=0:int
// library.kt:57 box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass, this_\58:MyClass=MyClass, $i$f$f1\58\500:int=0:int, $i$a$-f1-LibraryKt$foo$1\60\504\56:int=0:int, $i$f$test\61\500:int=0:int
// library.kt:58 box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass, this_\58:MyClass=MyClass, $i$f$f1\58\500:int=0:int, $i$a$-f1-LibraryKt$foo$1\60\504\56:int=0:int, $i$f$test\61\500:int=0:int, testVal\61:int=1:int
// library.kt:25 box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass, this_\58:MyClass=MyClass, $i$f$f1\58\500:int=0:int, $i$a$-f1-LibraryKt$foo$1\60\504\56:int=0:int
// library.kt:9 box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass, this_\58:MyClass=MyClass, $i$f$f1\58\500:int=0:int
// library.kt:10 box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass, this_\58:MyClass=MyClass, $i$f$f1\58\500:int=0:int
// library.kt:26 box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass
// library.kt:14 box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass, this_\62:MyClass=MyClass, $i$f$f2\62\506:int=0:int
// library.kt:57 box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass, this_\62:MyClass=MyClass, $i$f$f2\62\506:int=0:int, $i$f$test\63\507:int=0:int
// library.kt:58 box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass, this_\62:MyClass=MyClass, $i$f$f2\62\506:int=0:int, $i$f$test\63\507:int=0:int, testVal\63:int=1:int
// library.kt:15 box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass, this_\62:MyClass=MyClass, $i$f$f2\62\506:int=0:int
// library.kt:26 box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass, this_\62:MyClass=MyClass, $i$f$f2\62\506:int=0:int, $i$a$-f2-LibraryKt$foo$2\64\510\56:int=0:int
// library.kt:57 box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass, this_\62:MyClass=MyClass, $i$f$f2\62\506:int=0:int, $i$a$-f2-LibraryKt$foo$2\64\510\56:int=0:int, $i$f$test\65\506:int=0:int
// library.kt:58 box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass, this_\62:MyClass=MyClass, $i$f$f2\62\506:int=0:int, $i$a$-f2-LibraryKt$foo$2\64\510\56:int=0:int, $i$f$test\65\506:int=0:int, testVal\65:int=1:int
// library.kt:26 box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass, this_\62:MyClass=MyClass, $i$f$f2\62\506:int=0:int, $i$a$-f2-LibraryKt$foo$2\64\510\56:int=0:int
// library.kt:15 box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass, this_\62:MyClass=MyClass, $i$f$f2\62\506:int=0:int
// library.kt:16 box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass, this_\62:MyClass=MyClass, $i$f$f2\62\506:int=0:int
// library.kt:28 box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass
// library.kt:57 box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass, $i$f$test\66\512:int=0:int
// library.kt:58 box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass, $i$f$test\66\512:int=0:int, testVal\66:int=1:int
// library.kt:29 box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass
// library.kt:8 box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass, this_\67:MyClass=MyClass, $i$f$f1\67\515:int=0:int
// library.kt:57 box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass, this_\67:MyClass=MyClass, $i$f$f1\67\515:int=0:int, $i$f$test\68\516:int=0:int
// library.kt:58 box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass, this_\67:MyClass=MyClass, $i$f$f1\67\515:int=0:int, $i$f$test\68\516:int=0:int, testVal\68:int=1:int
// library.kt:9 box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass, this_\67:MyClass=MyClass, $i$f$f1\67\515:int=0:int
// library.kt:29 box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass, this_\67:MyClass=MyClass, $i$f$f1\67\515:int=0:int, $i$a$-f1-LibraryKt$foo$3\69\519\56:int=0:int
// library.kt:57 box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass, this_\67:MyClass=MyClass, $i$f$f1\67\515:int=0:int, $i$a$-f1-LibraryKt$foo$3\69\519\56:int=0:int, $i$f$test\70\515:int=0:int
// library.kt:58 box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass, this_\67:MyClass=MyClass, $i$f$f1\67\515:int=0:int, $i$a$-f1-LibraryKt$foo$3\69\519\56:int=0:int, $i$f$test\70\515:int=0:int, testVal\70:int=1:int
// library.kt:29 box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass, this_\67:MyClass=MyClass, $i$f$f1\67\515:int=0:int, $i$a$-f1-LibraryKt$foo$3\69\519\56:int=0:int
// library.kt:9 box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass, this_\67:MyClass=MyClass, $i$f$f1\67\515:int=0:int
// library.kt:10 box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass, this_\67:MyClass=MyClass, $i$f$f1\67\515:int=0:int
// library.kt:30 box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass
// library.kt:14 box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass, this_\71:MyClass=MyClass, $i$f$f2\71\521:int=0:int
// library.kt:57 box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass, this_\71:MyClass=MyClass, $i$f$f2\71\521:int=0:int, $i$f$test\72\522:int=0:int
// library.kt:58 box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass, this_\71:MyClass=MyClass, $i$f$f2\71\521:int=0:int, $i$f$test\72\522:int=0:int, testVal\72:int=1:int
// library.kt:15 box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass, this_\71:MyClass=MyClass, $i$f$f2\71\521:int=0:int
// library.kt:30 box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass, this_\71:MyClass=MyClass, $i$f$f2\71\521:int=0:int, $i$a$-f2-LibraryKt$foo$4\73\525\56:int=0:int
// library.kt:57 box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass, this_\71:MyClass=MyClass, $i$f$f2\71\521:int=0:int, $i$a$-f2-LibraryKt$foo$4\73\525\56:int=0:int, $i$f$test\74\521:int=0:int
// library.kt:58 box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass, this_\71:MyClass=MyClass, $i$f$f2\71\521:int=0:int, $i$a$-f2-LibraryKt$foo$4\73\525\56:int=0:int, $i$f$test\74\521:int=0:int, testVal\74:int=1:int
// library.kt:30 box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass, this_\71:MyClass=MyClass, $i$f$f2\71\521:int=0:int, $i$a$-f2-LibraryKt$foo$4\73\525\56:int=0:int
// library.kt:15 box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass, this_\71:MyClass=MyClass, $i$f$f2\71\521:int=0:int
// library.kt:16 box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass, this_\71:MyClass=MyClass, $i$f$f2\71\521:int=0:int
// library.kt:32 box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass
// library.kt:57 box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass, $i$f$test\75\527:int=0:int
// library.kt:58 box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass, $i$f$test\75\527:int=0:int, testVal\75:int=1:int
// library.kt:33 box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass
// _Arrays.kt:... box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass, $this$map\76:java.lang.Object[]=java.lang.Integer[], $i$f$map\76\530:int=0:int
// _Arrays.kt:... box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass, $this$map\76:java.lang.Object[]=java.lang.Integer[], $i$f$map\76\530:int=0:int, $this$mapTo\77:java.lang.Object[]=java.lang.Integer[], destination\77:java.util.Collection=java.util.ArrayList, $i$f$mapTo\77\531:int=0:int
// _Arrays.kt:... box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass, $this$map\76:java.lang.Object[]=java.lang.Integer[], $i$f$map\76\530:int=0:int, $this$mapTo\77:java.lang.Object[]=java.lang.Integer[], destination\77:java.util.Collection=java.util.ArrayList, $i$f$mapTo\77\531:int=0:int, item\77:java.lang.Object=java.lang.Integer
// library.kt:34 box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass, $this$map\76:java.lang.Object[]=java.lang.Integer[], $i$f$map\76\530:int=0:int, $this$mapTo\77:java.lang.Object[]=java.lang.Integer[], destination\77:java.util.Collection=java.util.ArrayList, $i$f$mapTo\77\531:int=0:int, item\77:java.lang.Object=java.lang.Integer, it\78:int=1:int, $i$a$-map-LibraryKt$foo$5\78\533\56:int=0:int
// _Arrays.kt:... box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass, $this$map\76:java.lang.Object[]=java.lang.Integer[], $i$f$map\76\530:int=0:int, $this$mapTo\77:java.lang.Object[]=java.lang.Integer[], destination\77:java.util.Collection=java.util.ArrayList, $i$f$mapTo\77\531:int=0:int, item\77:java.lang.Object=java.lang.Integer
// _Arrays.kt:... box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass, $this$map\76:java.lang.Object[]=java.lang.Integer[], $i$f$map\76\530:int=0:int, $this$mapTo\77:java.lang.Object[]=java.lang.Integer[], destination\77:java.util.Collection=java.util.ArrayList, $i$f$mapTo\77\531:int=0:int
// _Arrays.kt:... box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass, $this$map\76:java.lang.Object[]=java.lang.Integer[], $i$f$map\76\530:int=0:int, $this$mapTo\77:java.lang.Object[]=java.lang.Integer[], destination\77:java.util.Collection=java.util.ArrayList, $i$f$mapTo\77\531:int=0:int, item\77:java.lang.Object=java.lang.Integer
// library.kt:34 box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass, $this$map\76:java.lang.Object[]=java.lang.Integer[], $i$f$map\76\530:int=0:int, $this$mapTo\77:java.lang.Object[]=java.lang.Integer[], destination\77:java.util.Collection=java.util.ArrayList, $i$f$mapTo\77\531:int=0:int, item\77:java.lang.Object=java.lang.Integer, it\78:int=2:int, $i$a$-map-LibraryKt$foo$5\78\533\56:int=0:int
// _Arrays.kt:... box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass, $this$map\76:java.lang.Object[]=java.lang.Integer[], $i$f$map\76\530:int=0:int, $this$mapTo\77:java.lang.Object[]=java.lang.Integer[], destination\77:java.util.Collection=java.util.ArrayList, $i$f$mapTo\77\531:int=0:int, item\77:java.lang.Object=java.lang.Integer
// _Arrays.kt:... box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass, $this$map\76:java.lang.Object[]=java.lang.Integer[], $i$f$map\76\530:int=0:int, $this$mapTo\77:java.lang.Object[]=java.lang.Integer[], destination\77:java.util.Collection=java.util.ArrayList, $i$f$mapTo\77\531:int=0:int
// _Arrays.kt:... box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass, $this$map\76:java.lang.Object[]=java.lang.Integer[], $i$f$map\76\530:int=0:int, $this$mapTo\77:java.lang.Object[]=java.lang.Integer[], destination\77:java.util.Collection=java.util.ArrayList, $i$f$mapTo\77\531:int=0:int
// _Arrays.kt:... box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass, $this$map\76:java.lang.Object[]=java.lang.Integer[], $i$f$map\76\530:int=0:int
// library.kt:37 box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass
// library.kt:57 box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass, $i$f$test\79\536:int=0:int
// library.kt:58 box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass, $i$f$test\79\536:int=0:int, testVal\79:int=1:int
// library.kt:38 box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass
// _Arrays.kt:... box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass, $this$map\80:java.lang.Object[]=java.lang.Integer[], $i$f$map\80\539:int=0:int
// _Arrays.kt:... box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass, $this$map\80:java.lang.Object[]=java.lang.Integer[], $i$f$map\80\539:int=0:int, $this$mapTo\81:java.lang.Object[]=java.lang.Integer[], destination\81:java.util.Collection=java.util.ArrayList, $i$f$mapTo\81\540:int=0:int
// _Arrays.kt:... box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass, $this$map\80:java.lang.Object[]=java.lang.Integer[], $i$f$map\80\539:int=0:int, $this$mapTo\81:java.lang.Object[]=java.lang.Integer[], destination\81:java.util.Collection=java.util.ArrayList, $i$f$mapTo\81\540:int=0:int, item\81:java.lang.Object=java.lang.Integer
// library.kt:38 box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass, $this$map\80:java.lang.Object[]=java.lang.Integer[], $i$f$map\80\539:int=0:int, $this$mapTo\81:java.lang.Object[]=java.lang.Integer[], destination\81:java.util.Collection=java.util.ArrayList, $i$f$mapTo\81\540:int=0:int, item\81:java.lang.Object=java.lang.Integer, it\82:int=1:int, $i$a$-map-LibraryKt$foo$6\82\542\56:int=0:int
// _Arrays.kt:... box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass, $this$map\80:java.lang.Object[]=java.lang.Integer[], $i$f$map\80\539:int=0:int, $this$mapTo\81:java.lang.Object[]=java.lang.Integer[], destination\81:java.util.Collection=java.util.ArrayList, $i$f$mapTo\81\540:int=0:int, item\81:java.lang.Object=java.lang.Integer
// _Arrays.kt:... box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass, $this$map\80:java.lang.Object[]=java.lang.Integer[], $i$f$map\80\539:int=0:int, $this$mapTo\81:java.lang.Object[]=java.lang.Integer[], destination\81:java.util.Collection=java.util.ArrayList, $i$f$mapTo\81\540:int=0:int
// _Arrays.kt:... box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass, $this$map\80:java.lang.Object[]=java.lang.Integer[], $i$f$map\80\539:int=0:int, $this$mapTo\81:java.lang.Object[]=java.lang.Integer[], destination\81:java.util.Collection=java.util.ArrayList, $i$f$mapTo\81\540:int=0:int, item\81:java.lang.Object=java.lang.Integer
// library.kt:38 box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass, $this$map\80:java.lang.Object[]=java.lang.Integer[], $i$f$map\80\539:int=0:int, $this$mapTo\81:java.lang.Object[]=java.lang.Integer[], destination\81:java.util.Collection=java.util.ArrayList, $i$f$mapTo\81\540:int=0:int, item\81:java.lang.Object=java.lang.Integer, it\82:int=2:int, $i$a$-map-LibraryKt$foo$6\82\542\56:int=0:int
// _Arrays.kt:... box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass, $this$map\80:java.lang.Object[]=java.lang.Integer[], $i$f$map\80\539:int=0:int, $this$mapTo\81:java.lang.Object[]=java.lang.Integer[], destination\81:java.util.Collection=java.util.ArrayList, $i$f$mapTo\81\540:int=0:int, item\81:java.lang.Object=java.lang.Integer
// _Arrays.kt:... box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass, $this$map\80:java.lang.Object[]=java.lang.Integer[], $i$f$map\80\539:int=0:int, $this$mapTo\81:java.lang.Object[]=java.lang.Integer[], destination\81:java.util.Collection=java.util.ArrayList, $i$f$mapTo\81\540:int=0:int
// _Arrays.kt:... box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass, $this$map\80:java.lang.Object[]=java.lang.Integer[], $i$f$map\80\539:int=0:int, $this$mapTo\81:java.lang.Object[]=java.lang.Integer[], destination\81:java.util.Collection=java.util.ArrayList, $i$f$mapTo\81\540:int=0:int
// _Arrays.kt:... box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass, $this$map\80:java.lang.Object[]=java.lang.Integer[], $i$f$map\80\539:int=0:int
// library.kt:39 box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass
// _Collections.kt:... box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass, $this$filter\83:java.lang.Iterable=java.util.ArrayList, $i$f$filter\83\544:int=0:int
// _Collections.kt:... box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass, $this$filter\83:java.lang.Iterable=java.util.ArrayList, $i$f$filter\83\544:int=0:int, $this$filterTo\84:java.lang.Iterable=java.util.ArrayList, destination\84:java.util.Collection=java.util.ArrayList, $i$f$filterTo\84\545:int=0:int
// library.kt:40 box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass, $this$filter\83:java.lang.Iterable=java.util.ArrayList, $i$f$filter\83\544:int=0:int, $this$filterTo\84:java.lang.Iterable=java.util.ArrayList, destination\84:java.util.Collection=java.util.ArrayList, $i$f$filterTo\84\545:int=0:int, element\84:java.lang.Object=java.lang.Integer, it\85:int=2:int, $i$a$-filter-LibraryKt$foo$7\85\546\56:int=0:int
// _Collections.kt:... box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass, $this$filter\83:java.lang.Iterable=java.util.ArrayList, $i$f$filter\83\544:int=0:int, $this$filterTo\84:java.lang.Iterable=java.util.ArrayList, destination\84:java.util.Collection=java.util.ArrayList, $i$f$filterTo\84\545:int=0:int, element\84:java.lang.Object=java.lang.Integer
// library.kt:40 box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass, $this$filter\83:java.lang.Iterable=java.util.ArrayList, $i$f$filter\83\544:int=0:int, $this$filterTo\84:java.lang.Iterable=java.util.ArrayList, destination\84:java.util.Collection=java.util.ArrayList, $i$f$filterTo\84\545:int=0:int, element\84:java.lang.Object=java.lang.Integer, it\85:int=4:int, $i$a$-filter-LibraryKt$foo$7\85\546\56:int=0:int
// _Collections.kt:... box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass, $this$filter\83:java.lang.Iterable=java.util.ArrayList, $i$f$filter\83\544:int=0:int, $this$filterTo\84:java.lang.Iterable=java.util.ArrayList, destination\84:java.util.Collection=java.util.ArrayList, $i$f$filterTo\84\545:int=0:int, element\84:java.lang.Object=java.lang.Integer
// _Collections.kt:... box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass, $this$filter\83:java.lang.Iterable=java.util.ArrayList, $i$f$filter\83\544:int=0:int, $this$filterTo\84:java.lang.Iterable=java.util.ArrayList, destination\84:java.util.Collection=java.util.ArrayList, $i$f$filterTo\84\545:int=0:int
// _Collections.kt:... box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass, $this$filter\83:java.lang.Iterable=java.util.ArrayList, $i$f$filter\83\544:int=0:int
// library.kt:43 box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass
// library.kt:57 box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass, $i$f$test\86\549:int=0:int
// library.kt:58 box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass, $i$f$test\86\549:int=0:int, testVal\86:int=1:int
// library.kt:44 box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass
// _Arrays.kt:... box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass, $this$map\87:java.lang.Object[]=java.lang.Integer[], $i$f$map\87\552:int=0:int
// _Arrays.kt:... box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass, $this$map\87:java.lang.Object[]=java.lang.Integer[], $i$f$map\87\552:int=0:int, $this$mapTo\88:java.lang.Object[]=java.lang.Integer[], destination\88:java.util.Collection=java.util.ArrayList, $i$f$mapTo\88\553:int=0:int
// _Arrays.kt:... box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass, $this$map\87:java.lang.Object[]=java.lang.Integer[], $i$f$map\87\552:int=0:int, $this$mapTo\88:java.lang.Object[]=java.lang.Integer[], destination\88:java.util.Collection=java.util.ArrayList, $i$f$mapTo\88\553:int=0:int, item\88:java.lang.Object=java.lang.Integer
// library.kt:44 box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass, $this$map\87:java.lang.Object[]=java.lang.Integer[], $i$f$map\87\552:int=0:int, $this$mapTo\88:java.lang.Object[]=java.lang.Integer[], destination\88:java.util.Collection=java.util.ArrayList, $i$f$mapTo\88\553:int=0:int, item\88:java.lang.Object=java.lang.Integer, it\89:int=1:int, $i$a$-map-LibraryKt$foo$8\89\555\56:int=0:int
// _Arrays.kt:... box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass, $this$map\87:java.lang.Object[]=java.lang.Integer[], $i$f$map\87\552:int=0:int, $this$mapTo\88:java.lang.Object[]=java.lang.Integer[], destination\88:java.util.Collection=java.util.ArrayList, $i$f$mapTo\88\553:int=0:int, item\88:java.lang.Object=java.lang.Integer
// _Arrays.kt:... box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass, $this$map\87:java.lang.Object[]=java.lang.Integer[], $i$f$map\87\552:int=0:int, $this$mapTo\88:java.lang.Object[]=java.lang.Integer[], destination\88:java.util.Collection=java.util.ArrayList, $i$f$mapTo\88\553:int=0:int
// _Arrays.kt:... box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass, $this$map\87:java.lang.Object[]=java.lang.Integer[], $i$f$map\87\552:int=0:int, $this$mapTo\88:java.lang.Object[]=java.lang.Integer[], destination\88:java.util.Collection=java.util.ArrayList, $i$f$mapTo\88\553:int=0:int, item\88:java.lang.Object=java.lang.Integer
// library.kt:44 box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass, $this$map\87:java.lang.Object[]=java.lang.Integer[], $i$f$map\87\552:int=0:int, $this$mapTo\88:java.lang.Object[]=java.lang.Integer[], destination\88:java.util.Collection=java.util.ArrayList, $i$f$mapTo\88\553:int=0:int, item\88:java.lang.Object=java.lang.Integer, it\89:int=2:int, $i$a$-map-LibraryKt$foo$8\89\555\56:int=0:int
// _Arrays.kt:... box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass, $this$map\87:java.lang.Object[]=java.lang.Integer[], $i$f$map\87\552:int=0:int, $this$mapTo\88:java.lang.Object[]=java.lang.Integer[], destination\88:java.util.Collection=java.util.ArrayList, $i$f$mapTo\88\553:int=0:int, item\88:java.lang.Object=java.lang.Integer
// _Arrays.kt:... box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass, $this$map\87:java.lang.Object[]=java.lang.Integer[], $i$f$map\87\552:int=0:int, $this$mapTo\88:java.lang.Object[]=java.lang.Integer[], destination\88:java.util.Collection=java.util.ArrayList, $i$f$mapTo\88\553:int=0:int
// _Arrays.kt:... box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass, $this$map\87:java.lang.Object[]=java.lang.Integer[], $i$f$map\87\552:int=0:int, $this$mapTo\88:java.lang.Object[]=java.lang.Integer[], destination\88:java.util.Collection=java.util.ArrayList, $i$f$mapTo\88\553:int=0:int
// _Arrays.kt:... box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass, $this$map\87:java.lang.Object[]=java.lang.Integer[], $i$f$map\87\552:int=0:int
// library.kt:45 box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass
// _Collections.kt:... box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass, $this$filter\90:java.lang.Iterable=java.util.ArrayList, $i$f$filter\90\557:int=0:int
// _Collections.kt:... box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass, $this$filter\90:java.lang.Iterable=java.util.ArrayList, $i$f$filter\90\557:int=0:int, $this$filterTo\91:java.lang.Iterable=java.util.ArrayList, destination\91:java.util.Collection=java.util.ArrayList, $i$f$filterTo\91\558:int=0:int
// library.kt:46 box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass, $this$filter\90:java.lang.Iterable=java.util.ArrayList, $i$f$filter\90\557:int=0:int, $this$filterTo\91:java.lang.Iterable=java.util.ArrayList, destination\91:java.util.Collection=java.util.ArrayList, $i$f$filterTo\91\558:int=0:int, element\91:java.lang.Object=java.lang.Integer, it\92:int=2:int, $i$a$-filter-LibraryKt$foo$9\92\559\56:int=0:int
// _Collections.kt:... box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass, $this$filter\90:java.lang.Iterable=java.util.ArrayList, $i$f$filter\90\557:int=0:int, $this$filterTo\91:java.lang.Iterable=java.util.ArrayList, destination\91:java.util.Collection=java.util.ArrayList, $i$f$filterTo\91\558:int=0:int, element\91:java.lang.Object=java.lang.Integer
// library.kt:46 box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass, $this$filter\90:java.lang.Iterable=java.util.ArrayList, $i$f$filter\90\557:int=0:int, $this$filterTo\91:java.lang.Iterable=java.util.ArrayList, destination\91:java.util.Collection=java.util.ArrayList, $i$f$filterTo\91\558:int=0:int, element\91:java.lang.Object=java.lang.Integer, it\92:int=4:int, $i$a$-filter-LibraryKt$foo$9\92\559\56:int=0:int
// _Collections.kt:... box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass, $this$filter\90:java.lang.Iterable=java.util.ArrayList, $i$f$filter\90\557:int=0:int, $this$filterTo\91:java.lang.Iterable=java.util.ArrayList, destination\91:java.util.Collection=java.util.ArrayList, $i$f$filterTo\91\558:int=0:int, element\91:java.lang.Object=java.lang.Integer
// _Collections.kt:... box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass, $this$filter\90:java.lang.Iterable=java.util.ArrayList, $i$f$filter\90\557:int=0:int, $this$filterTo\91:java.lang.Iterable=java.util.ArrayList, destination\91:java.util.Collection=java.util.ArrayList, $i$f$filterTo\91\558:int=0:int
// _Collections.kt:... box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass, $this$filter\90:java.lang.Iterable=java.util.ArrayList, $i$f$filter\90\557:int=0:int
// library.kt:49 box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass
// library.kt:57 box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass, $i$f$test\93\562:int=0:int
// library.kt:58 box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass, $i$f$test\93\562:int=0:int, testVal\93:int=1:int
// library.kt:50 box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass
// library.kt:8 box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass, this_\94:MyClass=MyClass, $i$f$f1\94\563:int=0:int
// library.kt:57 box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass, this_\94:MyClass=MyClass, $i$f$f1\94\563:int=0:int, $i$f$test\95\572:int=0:int
// library.kt:58 box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass, this_\94:MyClass=MyClass, $i$f$f1\94\563:int=0:int, $i$f$test\95\572:int=0:int, testVal\95:int=1:int
// library.kt:9 box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass, this_\94:MyClass=MyClass, $i$f$f1\94\563:int=0:int
// library.kt:50 box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass, this_\94:MyClass=MyClass, $i$f$f1\94\563:int=0:int, $i$a$-f1-LibraryKt$foo$10\96\573\56:int=0:int
// library.kt:57 box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass, this_\94:MyClass=MyClass, $i$f$f1\94\563:int=0:int, $i$a$-f1-LibraryKt$foo$10\96\573\56:int=0:int, $i$f$test\97\563:int=0:int
// library.kt:58 box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass, this_\94:MyClass=MyClass, $i$f$f1\94\563:int=0:int, $i$a$-f1-LibraryKt$foo$10\96\573\56:int=0:int, $i$f$test\97\563:int=0:int, testVal\97:int=1:int
// library.kt:50 box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass, this_\94:MyClass=MyClass, $i$f$f1\94\563:int=0:int, $i$a$-f1-LibraryKt$foo$10\96\573\56:int=0:int
// library.kt:9 box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass, this_\94:MyClass=MyClass, $i$f$f1\94\563:int=0:int
// library.kt:10 box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass, this_\94:MyClass=MyClass, $i$f$f1\94\563:int=0:int
// library.kt:50 box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass
// library.kt:14 box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass, this_\98:MyClass=MyClass, $i$f$f2\98\563:int=0:int
// library.kt:57 box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass, this_\98:MyClass=MyClass, $i$f$f2\98\563:int=0:int, $i$f$test\99\578:int=0:int
// library.kt:58 box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass, this_\98:MyClass=MyClass, $i$f$f2\98\563:int=0:int, $i$f$test\99\578:int=0:int, testVal\99:int=1:int
// library.kt:15 box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass, this_\98:MyClass=MyClass, $i$f$f2\98\563:int=0:int
// library.kt:50 box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass, this_\98:MyClass=MyClass, $i$f$f2\98\563:int=0:int, $i$a$-f2-LibraryKt$foo$11\100\579\56:int=0:int
// library.kt:57 box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass, this_\98:MyClass=MyClass, $i$f$f2\98\563:int=0:int, $i$a$-f2-LibraryKt$foo$11\100\579\56:int=0:int, $i$f$test\101\563:int=0:int
// library.kt:58 box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass, this_\98:MyClass=MyClass, $i$f$f2\98\563:int=0:int, $i$a$-f2-LibraryKt$foo$11\100\579\56:int=0:int, $i$f$test\101\563:int=0:int, testVal\101:int=1:int
// library.kt:50 box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass, this_\98:MyClass=MyClass, $i$f$f2\98\563:int=0:int, $i$a$-f2-LibraryKt$foo$11\100\579\56:int=0:int
// library.kt:15 box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass, this_\98:MyClass=MyClass, $i$f$f2\98\563:int=0:int
// library.kt:16 box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass, this_\98:MyClass=MyClass, $i$f$f2\98\563:int=0:int
// library.kt:52 box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass
// library.kt:57 box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass, $i$f$test\102\565:int=0:int
// library.kt:58 box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass, $i$f$test\102\565:int=0:int, testVal\102:int=1:int
// library.kt:53 box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass
// library.kt:8 box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass, this_\103:MyClass=MyClass, $i$f$f1\103\566:int=0:int
// library.kt:57 box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass, this_\103:MyClass=MyClass, $i$f$f1\103\566:int=0:int, $i$f$test\104\572:int=0:int
// library.kt:58 box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass, this_\103:MyClass=MyClass, $i$f$f1\103\566:int=0:int, $i$f$test\104\572:int=0:int, testVal\104:int=1:int
// library.kt:9 box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass, this_\103:MyClass=MyClass, $i$f$f1\103\566:int=0:int
// library.kt:53 box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass, this_\103:MyClass=MyClass, $i$f$f1\103\566:int=0:int, $i$a$-f1-LibraryKt$foo$12\105\573\56:int=0:int
// library.kt:57 box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass, this_\103:MyClass=MyClass, $i$f$f1\103\566:int=0:int, $i$a$-f1-LibraryKt$foo$12\105\573\56:int=0:int, $i$f$test\106\566:int=0:int
// library.kt:58 box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass, this_\103:MyClass=MyClass, $i$f$f1\103\566:int=0:int, $i$a$-f1-LibraryKt$foo$12\105\573\56:int=0:int, $i$f$test\106\566:int=0:int, testVal\106:int=1:int
// library.kt:53 box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass, this_\103:MyClass=MyClass, $i$f$f1\103\566:int=0:int, $i$a$-f1-LibraryKt$foo$12\105\573\56:int=0:int
// library.kt:9 box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass, this_\103:MyClass=MyClass, $i$f$f1\103\566:int=0:int
// library.kt:10 box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass, this_\103:MyClass=MyClass, $i$f$f1\103\566:int=0:int
// library.kt:53 box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass
// library.kt:14 box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass, this_\107:MyClass=MyClass, $i$f$f2\107\566:int=0:int
// library.kt:57 box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass, this_\107:MyClass=MyClass, $i$f$f2\107\566:int=0:int, $i$f$test\108\578:int=0:int
// library.kt:58 box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass, this_\107:MyClass=MyClass, $i$f$f2\107\566:int=0:int, $i$f$test\108\578:int=0:int, testVal\108:int=1:int
// library.kt:15 box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass, this_\107:MyClass=MyClass, $i$f$f2\107\566:int=0:int
// library.kt:53 box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass, this_\107:MyClass=MyClass, $i$f$f2\107\566:int=0:int, $i$a$-f2-LibraryKt$foo$13\109\579\56:int=0:int
// library.kt:57 box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass, this_\107:MyClass=MyClass, $i$f$f2\107\566:int=0:int, $i$a$-f2-LibraryKt$foo$13\109\579\56:int=0:int, $i$f$test\110\566:int=0:int
// library.kt:58 box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass, this_\107:MyClass=MyClass, $i$f$f2\107\566:int=0:int, $i$a$-f2-LibraryKt$foo$13\109\579\56:int=0:int, $i$f$test\110\566:int=0:int, testVal\110:int=1:int
// library.kt:53 box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass, this_\107:MyClass=MyClass, $i$f$f2\107\566:int=0:int, $i$a$-f2-LibraryKt$foo$13\109\579\56:int=0:int
// library.kt:15 box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass, this_\107:MyClass=MyClass, $i$f$f2\107\566:int=0:int
// library.kt:16 box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass, this_\107:MyClass=MyClass, $i$f$f2\107\566:int=0:int
// library.kt:54 box: $i$f$foo\56\65:int=0:int, array\56:java.lang.Integer[]=java.lang.Integer[], myClass\56:MyClass=MyClass
// test.kt:66 box:
