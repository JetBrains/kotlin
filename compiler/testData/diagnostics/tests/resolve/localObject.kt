fun foo(): Any {
    <!LOCAL_OBJECT_NOT_ALLOWED!>object Bar<!>
    return Bar
}