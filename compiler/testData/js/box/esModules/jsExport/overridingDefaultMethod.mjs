import * as api from "./overridingDefaultMethod_v5.mjs";

export default function() {
    const baseClass = new api.BaseClass()
    const subClass = new api.SubClass()

    return {
        baseClassResult: baseClass.sayHello(),
        subClassResult: subClass.sayHello(),
    };
};
