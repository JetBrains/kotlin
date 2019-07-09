import p.*

fun foo(): KotlinTrait<I1, I2> {
    return <caret>
}

// EXIST: { lookupString: "object", itemText: "object : KotlinTrait<I1, I2>{...}" }
// ABSENT: KotlinInheritor
