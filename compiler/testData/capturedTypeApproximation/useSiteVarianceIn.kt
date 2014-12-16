class Inv<T>

fun foo<T>(a: Inv<in T>) {}

// T captures 'in Int'
// lower: Inv<Any?>
// upper: Inv<in Int>

// T captures 'out Int'
// lower: Inv<in Int>
// upper: Inv<out Any?>
