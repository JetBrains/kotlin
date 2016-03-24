import kotlin.properties.Delegates

class C {
    val v by Delegates.<caret>
}

// EXIST: notNull
// EXIST: observable
// EXIST: vetoable
// NOTHING_ELSE
