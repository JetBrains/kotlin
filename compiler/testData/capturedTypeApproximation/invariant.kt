class Inv<T>

fun foo<T>(a: Inv<T>) {}

// T captures 'in Int'
// lower: Nothing
// upper: Inv<in Int>

// T captures 'out Int'
// lower: Nothing
// upper: Inv<out Int>
