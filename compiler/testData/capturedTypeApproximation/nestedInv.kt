class Inv<T>

fun foo<T>(a: Inv<Inv<T>>) {}

// T captures 'in Int'
// lower: Nothing
// upper: Inv<out Inv<in Int>>

// T captures 'out Int'
// lower: Nothing
// upper: Inv<out Inv<out Int>>
