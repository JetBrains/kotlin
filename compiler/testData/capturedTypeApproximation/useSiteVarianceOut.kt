class Inv<T>

fun foo<T>(a: Inv<out T>) {}

// T captures 'in Int'
// lower: Inv<out Int>
// upper: Inv<out Any?>

// T captures 'out Int'
// lower: Inv<Nothing>
// upper: Inv<out Int>
