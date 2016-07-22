/**
 * Created by user on 7/21/16.
 */

const main = require("./../main.js");

function handle(httpContent, response) {
    const routeClass = main.protoConstructorRoute.RouteRequest;
    const routeObject = routeClass.decode(httpContent);
    const wayPoints = routeObject.way_points;
    main.thisCar.setPath(wayPoints);

    var routeResponse = new main.protoConstructorRoute.RouteResponse({
        "code": 0,
        "errorMsg": ""
    });

    var byteBuffer = routeResponse.encode();
    var byteArray = [];
    for (var i = 0; i < byteBuffer.limit; i++) {
        byteArray.push(byteBuffer.buffer[i]);
    }

    response.writeHead(200, {"Content-Type": "text/plain", "Content-length": byteArray.length});
    response.write(new Buffer(byteArray));
    response.end();

}

exports.handler = handle;