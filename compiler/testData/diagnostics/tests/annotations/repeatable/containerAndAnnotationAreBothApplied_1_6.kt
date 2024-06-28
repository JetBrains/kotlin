// FIR_IDENTICAL
// LANGUAGE: +RepeatableAnnotations
// FULL_JDK
// FILE: JR.java

import java.lang.annotation.*;

@Repeatable(JR.Container.class)
@Retention(RetentionPolicy.RUNTIME)
public @interface JR {
    public @interface Container {
        JR[] value();
    }
}

// FILE: JS.java

import java.lang.annotation.*;

@Repeatable(JS.Container.class)
@Retention(RetentionPolicy.SOURCE)
public @interface JS {
    public @interface Container {
        JS[] value();
    }
}

// FILE: KR.kt

@java.lang.annotation.Repeatable(KR.Container::class)
@Retention(AnnotationRetention.RUNTIME)
annotation class KR {
    annotation class Container(val value: Array<KR>)
}

// FILE: KS.kt

@java.lang.annotation.Repeatable(KS.Container::class)
@Retention(AnnotationRetention.SOURCE)
annotation class KS {
    annotation class Container(val value: Array<KS>)
}

// FILE: test.kt

// Java runtime-retained annotation

@JR
@JR.Container()
fun jr1() {}

@JR
@JR.Container()
<!REPEATED_ANNOTATION_WITH_CONTAINER!>@JR<!>
fun jr2() {}

@JR
@JR.Container(JR())
<!REPEATED_ANNOTATION_WITH_CONTAINER!>@JR<!>
fun jr3() {}

@JR
@JR.Container(JR())
fun jr4() {}

@JR
@JR.Container(JR(), JR())
fun jr5() {}


// Java source-retained annotation

@JS
@JS.Container()
fun js1() {}

@JS
@JS.Container()
@JS
fun js2() {}

@JS
@JS.Container(JS())
@JS
fun js3() {}

@JS
@JS.Container(JS())
fun js4() {}

@JS
@JS.Container(JS(), JS())
fun js5() {}


// Kotlin runtime-retained annotation

@KR.Container([])
@KR
fun kr1() {}

@KR.Container([])
@KR
<!REPEATED_ANNOTATION_WITH_CONTAINER!>@KR<!>
fun kr2() {}

@KR
<!REPEATED_ANNOTATION_WITH_CONTAINER!>@KR<!>
@KR.Container([KR()])
fun kr3() {}

@KR.Container([KR()])
@KR
fun kr4() {}

@KR
@KR.Container([KR(), KR()])
fun kr5() {}


// Kotlin source-retained annotation

@KS.Container([])
@KS
fun ks1() {}

@KS.Container([])
@KS
@KS
fun ks2() {}

@KS
@KS
@KS.Container([KS()])
fun ks3() {}

@KS.Container([KS()])
@KS
fun ks4() {}

@KS
@KS.Container([KS(), KS()])
fun ks5() {}
