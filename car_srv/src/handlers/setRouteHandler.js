/**
 * Created by user on 7/21/16.
 */

const main = require("./../main.js");

function handle(httpContent, response) {
    var routeClass = main.protoConstructorRoute.RouteRequest;
    var routeObject = routeClass.decode(httpContent);
    console.log(routeObject)//todo сделать
}

exports.handler = handle;