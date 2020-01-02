// !EXPLICIT_FLEXIBLE_TYPES

interface A<T>
interface B<T>: A<ft<T, T?>>

interface C: A<String>, B<String>
interface D: B<String>, A<String>
interface E: A<String?>, B<String?>
interface F: A<String?>, B<String>

interface G: A<String>, B<String?>
interface H: A<Int>, B<String>
interface I: B<Int>, A<String>