package p1

fun foo(): p2.KotlinTrait {
    return <caret>
}

// EXIST: { lookupString: "object", itemText: "object : KotlinTrait{...}" }
// EXIST: { lookupString: "KotlinInheritor1", itemText: "KotlinInheritor1", tailText: "() (p2)" }
// EXIST: { lookupString: "KotlinInheritor2", itemText: "KotlinInheritor2", tailText: "(s: String) (p2)" }
// EXIST: { lookupString: "object", itemText: "object : KotlinInheritor3(){...}" }
// ABSENT: PrivateInheritor
// EXIST: { lookupString: "object", itemText: "object : JavaInheritor1(){...}" }
// EXIST: { lookupString: "JavaInheritor2", itemText: "JavaInheritor2", tailText: "(...) (<root>)" }
// ABSENT: JavaInheritor3
// EXIST: { lookupString: "ObjectInheritor", itemText: "ObjectInheritor", tailText: " (p2)" }
