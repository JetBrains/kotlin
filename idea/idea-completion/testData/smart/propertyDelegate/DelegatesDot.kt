import kotlin.properties.Delegates

class C {
    val v by Delegates.<caret>
}

// EXIST: { itemText: "notNull", typeText: "ReadWriteProperty<Any?, T>" }
// EXIST: { itemText: "observable", typeText: "ReadWriteProperty<Any?, T>" }
// EXIST: { itemText: "vetoable", typeText: "ReadWriteProperty<Any?, T>" }
// NOTHING_ELSE
