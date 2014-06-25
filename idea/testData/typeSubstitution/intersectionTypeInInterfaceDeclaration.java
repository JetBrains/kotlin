interface intersectionTypeInInterfaceDeclaration {
    interface SuperIntersection<T extends Object & Cloneable> {
        T typeForSubstitute();
    }

    interface MidIntersection<U extends Object & Cloneable> extends SuperIntersection<U> {
    }
}