interface Simple {
    default String test(String s) {
        return s + "Fail";
    }
}