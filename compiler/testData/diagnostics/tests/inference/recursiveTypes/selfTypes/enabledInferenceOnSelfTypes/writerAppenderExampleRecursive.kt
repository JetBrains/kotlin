// LANGUAGE: +TypeInferenceOnCallsWithSelfTypes

// FILE: JavaWriterAppender.java
public class JavaWriterAppender {
    interface Builder2<K extends Builder2<K>> {}

    class Builder1<B extends Builder1<B>> {
        B asBuilder() { return null; }
    }

    <B extends Builder1<B>> B newBuilder() {
        return null;
    }

    <B extends Builder1<B> & Builder2<B>> B intersectTwoSelfTypes() {
        return null;
    }
}

// FILE: main.kt
fun test() {
    <!DEBUG_INFO_EXPRESSION_TYPE("WriterAppender.Builder1<*>")!>WriterAppender.newBuilder()<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("WriterAppender.Builder1<out WriterAppender.Builder1<*>>")!>WriterAppender.Builder1()<!>

    <!DEBUG_INFO_EXPRESSION_TYPE("{Builder1<*> & Builder2<*>}")!>WriterAppender.intersectTwoSelfTypes()<!>
}

fun testJava(appender: JavaWriterAppender) {
    <!DEBUG_INFO_EXPRESSION_TYPE("(JavaWriterAppender.Builder1<*>..JavaWriterAppender.Builder1<*>?)")!>appender.newBuilder()<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("JavaWriterAppender.Builder1<out (JavaWriterAppender.Builder1<*>..JavaWriterAppender.Builder1<*>?)>")!>appender.Builder1()<!>

    <!DEBUG_INFO_EXPRESSION_TYPE("({Builder1<*> & Builder2<*>}..{Builder1<*>? & Builder2<*>?}?)")!>appender.intersectTwoSelfTypes()<!>
}

object WriterAppender {
    interface Builder2<K : Builder2<K>>

    class Builder1<B : Builder1<B>> {
        fun asBuilder(): B {
            return this <!UNCHECKED_CAST!>as B<!>
        }
    }

    fun <B : Builder1<B>> newBuilder(): B {
        return Builder1<B>().asBuilder()
    }

    fun <B> intersectTwoSelfTypes(): B where B : Builder1<B>, B: Builder2<B> {
        return Builder1<B>().asBuilder()
    }
}