fun foo() {
    "OK".apply { this }
    "OK".apply2 { this }
}


inline fun String.apply2(f: String.() -> String) = this.f()
