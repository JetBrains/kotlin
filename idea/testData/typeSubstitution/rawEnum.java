interface rawEnum {
    interface Super<T extends Enum<T>> {
        Enum<T> typeForSubstitute();
    }

    interface Sub extends Super {
    }
}