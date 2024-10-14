// WITH_STDLIB
// LANGUAGE: +ValueClasses
// TARGET_BACKEND: JVM_IR

// MODULE: lib
// FILE: Sentence.kt

@JvmInline
value class Sentence(private val wholeText: String, internal val basis: Set<String>, public val x: Int, protected val y: Long) {
    fun op() = Sentence(wholeText, basis, -x, -y)
}

@JvmInline
value class NestedSentence(
    private val sentence1: Sentence, internal val sentence2: Sentence, public val sentence3: Sentence, protected val sentence4: Sentence
) {
    fun op() = NestedSentence(sentence1.op(), sentence2.op(), sentence3.op(), sentence4.op())
}

@JvmInline
value class NestedNestedSentence(
    private val sentence1: NestedSentence, internal val sentence2: NestedSentence,
    public val sentence3: NestedSentence, protected val sentence4: NestedSentence
)

data class MutableSentence(
    private var sentence1: NestedSentence, internal var sentence2: NestedSentence,
    public var sentence3: NestedSentence, protected var sentence4: NestedSentence
)

// MODULE: main(lib)
// FILE: main.kt
fun box(): String {
    val sentence = Sentence("file", setOf("package"), 2, 3L)
    if (sentence.toString() != "Sentence(wholeText=file, basis=[package], x=2, y=3)") {
        return sentence.toString()
    }
    if (sentence.x != 2) return sentence.x.toString()
    
    val nestedSentence = NestedSentence(sentence, sentence, sentence, sentence)
    if (nestedSentence.toString() != "NestedSentence(sentence1=$sentence, sentence2=$sentence, sentence3=$sentence, sentence4=$sentence)") {
        return nestedSentence.toString()
    }
    if (nestedSentence.sentence3 != sentence) return nestedSentence.sentence3.toString()
    if (nestedSentence.sentence3.x != sentence.x) return nestedSentence.sentence3.x.toString()

    val nestedNestedSentence = NestedNestedSentence(nestedSentence, nestedSentence, nestedSentence, nestedSentence)
    if (nestedNestedSentence.toString() != "NestedNestedSentence(sentence1=$nestedSentence, sentence2=$nestedSentence, sentence3=$nestedSentence, sentence4=$nestedSentence)") {
        return nestedNestedSentence.toString()
    }
    if (nestedNestedSentence.sentence3 != nestedSentence) return nestedNestedSentence.sentence3.toString()
    if (nestedNestedSentence.sentence3.sentence3 != sentence) return nestedNestedSentence.sentence3.sentence3.toString()
    if (nestedNestedSentence.sentence3.sentence3.x != sentence.x) return nestedNestedSentence.sentence3.sentence3.x.toString()

    val mutable = MutableSentence(nestedSentence, nestedSentence, nestedSentence, nestedSentence)
    if (mutable.toString() != "MutableSentence(sentence1=$nestedSentence, sentence2=$nestedSentence, sentence3=$nestedSentence, sentence4=$nestedSentence)") {
        return mutable.toString()
    }
    if (mutable.sentence3 != nestedSentence) return mutable.sentence3.toString()
    if (mutable.sentence3.sentence3 != sentence) return mutable.sentence3.sentence3.toString()
    if (mutable.sentence3.sentence3.x != sentence.x) return mutable.sentence3.sentence3.x.toString()
    
    mutable.sentence3 = mutable.sentence3.op()
    
    if (mutable == MutableSentence(nestedSentence, nestedSentence, nestedSentence, nestedSentence)) return mutable.toString()
    if (mutable.toString() == "MutableSentence(sentence1=$nestedSentence, sentence2=$nestedSentence, sentence3=$nestedSentence, sentence4=$nestedSentence)") {
        return mutable.toString()
    }
    if (mutable.sentence3 == nestedSentence) return mutable.sentence3.toString()
    if (mutable.sentence3.sentence3 == sentence) return mutable.sentence3.sentence3.toString()
    if (mutable.sentence3.sentence3.x == sentence.x) return mutable.sentence3.sentence3.x.toString()
    
    if (mutable.toString().replace("-", "") != "MutableSentence(sentence1=$nestedSentence, sentence2=$nestedSentence, sentence3=$nestedSentence, sentence4=$nestedSentence)") {
        return mutable.toString()
    }
    if (mutable.sentence3 != nestedSentence.op()) return mutable.sentence3.toString()
    if (mutable.sentence3.sentence3 != sentence.op()) return mutable.sentence3.sentence3.toString()
    if (mutable.sentence3.sentence3.x != -sentence.x) return mutable.sentence3.sentence3.x.toString()
    
    return "OK"
}
