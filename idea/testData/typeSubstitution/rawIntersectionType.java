interface rawIntersectionType {
    interface Super<T extends Integer & Cloneable> {
        T typeForSubstitute();
    }

    interface Sub extends Super {
    }
}