interface T {
    final fun component1(): Int = 42
}

<!DATA_CLASS_OVERRIDE_CONFLICT!>data<!> class A(val x: Int) : T
