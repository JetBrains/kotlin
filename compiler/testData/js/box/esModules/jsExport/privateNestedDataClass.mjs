import { TableDriver } from "./privateNestedDataClass_v5.mjs"

export default function box() {
    var tableDriver = new TableDriver();

    return {
        value: tableDriver.foo(),
        private: TableDriver.PrivateTable,
        public: TableDriver.PublicTable
    };
};