interface rawArrayTypeParameterWithBound {
    interface Super<T extends Cloneable> {
        T[] typeForSubstitute();
    }

    interface Sub extends Super {
    }
}