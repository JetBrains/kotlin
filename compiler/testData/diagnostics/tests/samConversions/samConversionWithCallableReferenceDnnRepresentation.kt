// LANGUAGE: +JavaTypeParameterDefaultRepresentationWithDNN

// FILE: Consumer.java
public interface Consumer<T> {
    void accept(T t);
}

// FILE: Producer.java
public interface Producer<T> {
    T produce();
}

// FILE: JavaBox.java
public class JavaBox<T> {
    public JavaBox(T b) {
        a = b;
    }
    public T a;
}

// FILE: Test.kt
private fun takeAny(x: Any) {}
private fun takeNullableAny(x: Any?) {}
private fun returnAny(): Any {
    return 1
}
private fun returnNullableAny(): Any? {
    return 1
}
fun returnNullableString(): String? {
    return "1"
}
private fun returnString(): String {
    return "1"
}

class A {
    fun doOnSuccessIn(consumer: Consumer<in String>) {
        consumer.accept(null)
    }
    fun doOnSuccessOut(consumer: Consumer<out String>) {
        consumer.accept(null)
    }
    fun doOnSuccessStar(consumer: Consumer<*>) {
        consumer.accept(null)
    }
    fun doOnSuccessString(consumer: Consumer<String>) {
        consumer.accept(null)
    }
    fun doOnSuccessNullableString(consumer: Consumer<String?>) {
        consumer.accept(null)
    }
    fun <T> doOnSuccessTypeParameter(consumer: Consumer<T>) {
        consumer.accept(null)
    }
    fun doOnSuccessJavaBox(consumer: Consumer<JavaBox<in String>>) {
        consumer.accept(null)
    }
    fun doOnSuccessJavaBox2(consumer: Consumer<JavaBox<String>>) {
        consumer.accept(null)
    }

    fun doOnSuccessInProducer(producer: Producer<in String>) {
        val k: Any? = producer.produce()
    }

    fun doOnSuccessOutProducer(producer: Producer<out String>) {
        val k: String = producer.produce()
    }

    fun doOnSuccessStarProducer(producer: Producer<*>) {
        val k: Any = producer.produce()
    }

    fun doOnSuccessStringProducer(producer: Producer<String>) {
        val k: String = producer.produce()
    }
    fun <T> doOnSuccessTypeParameterProducer(producer: Producer<T>) {
        val k: T = producer.produce()
    }
}

fun main() {
    A().doOnSuccessIn(::takeAny)
    A().doOnSuccessOut(::takeAny)
    A().doOnSuccessStar(::takeAny)
    A().doOnSuccessString(::takeAny)
    A().doOnSuccessNullableString(<!TYPE_MISMATCH!>::takeAny<!>)
    A().doOnSuccessNullableString(::takeNullableAny)
    A().doOnSuccessTypeParameter(::takeAny)
    A().doOnSuccessTypeParameter(::takeNullableAny)
    A().doOnSuccessTypeParameter<String>(::takeAny)
    A().doOnSuccessJavaBox(::takeAny)
    A().doOnSuccessJavaBox2(::takeAny)
    A().doOnSuccessJavaBox2(::takeNullableAny)

    A().doOnSuccessInProducer(<!TYPE_MISMATCH!>::returnAny<!>)
    A().doOnSuccessInProducer(::returnString)
    A().doOnSuccessInProducer(::returnNullableString)
    A().doOnSuccessOutProducer(::returnString)
    A().doOnSuccessOutProducer(::returnNullableString)
    A().doOnSuccessOutProducer(<!TYPE_MISMATCH!>::returnNullableAny<!>)

    A().doOnSuccessStarProducer(::returnNullableAny)
    A().doOnSuccessStarProducer(::returnAny)
    A().doOnSuccessStarProducer(::returnString)
    A().doOnSuccessStarProducer(::returnNullableString)
    A().doOnSuccessStringProducer(::returnString)
    A().doOnSuccessStringProducer(::returnNullableString)
    A().doOnSuccessStringProducer(<!TYPE_MISMATCH!>::returnNullableAny<!>)

    A().doOnSuccessTypeParameterProducer(::returnNullableAny)
    A().doOnSuccessTypeParameterProducer(::returnAny)
    A().doOnSuccessTypeParameterProducer(::returnNullableString)
    A().doOnSuccessTypeParameterProducer(::returnString)
    A().doOnSuccessTypeParameterProducer<String>(<!TYPE_MISMATCH!>::returnNullableString<!>)
}