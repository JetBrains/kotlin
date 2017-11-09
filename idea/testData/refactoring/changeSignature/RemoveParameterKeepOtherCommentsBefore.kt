data class <caret>VertexAttribute(

        /**
         * The number of components this attribute has.
         **/
        val numComponents: Int,

        /**
         * If true and [type] is not [Gl20.FLOAT], the data will be mapped to the range -1 to 1 for signed types and
         * the range 0 to 1 for unsigned types.
         */
        val normalized: Boolean
) {}