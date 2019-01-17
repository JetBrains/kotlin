(function(global) {
    var modules = {};
    modules.kotlin = kotlin;

    // Hard-code expected dependency order since we are unable to refer to modules by filename here.
    var names = ["jslib-example", "out"];

    function define(name, dependencies, body) {
        if (Array.isArray(name)) {
            body = dependencies;
            dependencies = name;
            name = names.shift();
        }
        else {
            if (name !== names.shift()) throw new Error("Unexpected dependency")
        }
        var resolvedDependencies = [];
        var currentModule = {};
        modules[name] = currentModule;
        for (var i = 0; i < dependencies.length; ++i) {
            var dependencyName = dependencies[i];
            resolvedDependencies[i] = dependencyName === 'exports' ? currentModule : modules[dependencyName];
        }
        currentModule = body.apply(body, resolvedDependencies);
        if (currentModule) {
            modules[name] = currentModule;
        }
    }
    define.amd = {};

    function require(name) {
        return modules[name];
    }

    global.define = define;
    global.$kotlin_test_internal$ = {
        require : require
    };
})(this);