// LANGUAGE: -TypeInferenceOnCallsWithSelfTypes

fun test() {
    <!DEBUG_INFO_EXPRESSION_TYPE("ERROR CLASS: Cannot infer argument for type parameter B")!>WriterAppender.<!CANNOT_INFER_PARAMETER_TYPE, NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>newBuilder<!>()<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("WriterAppender.Builder1<ERROR CLASS: Cannot infer argument for type parameter B>")!>WriterAppender.<!CANNOT_INFER_PARAMETER_TYPE, NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>Builder1<!>()<!>

    <!DEBUG_INFO_EXPRESSION_TYPE("ERROR CLASS: Cannot infer argument for type parameter B")!>WriterAppender.<!CANNOT_INFER_PARAMETER_TYPE, NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>intersectTwoSelfTypes<!>()<!>
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
